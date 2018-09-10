/*
 * Copyright (C) 2018 Marcus Hirt
 *                    www.hirt.se
 *
 * This software is free:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright (C) Marcus Hirt, 2018
 */
package se.hirt.examples.robotshop.loadgenerator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import se.hirt.examples.problematicwebapp.data.Customer;
import se.hirt.examples.problematicwebapp.rest.CustomerKeys;

/**
 * The thing actually generating some load.
 * 
 * @author Marcus Hirt
 */
public class LoadWorker implements Runnable {
	private final static Random RND = new Random();
	private final HttpClient client = HttpClient.newHttpClient();
	private final HttpClient deleteClient = HttpClient.newHttpClient();
	private final String[] baseUrls;

	public LoadWorker(Properties configuration) {
		baseUrls = configuration.getProperty("baseurls").split(", ");
	}

	@Override
	public void run() {
		putAndRemoveRandomCustomerAsync();
	}

	public String getRandomBaseURL() {
		return baseUrls[RND.nextInt(baseUrls.length)];
	}

	private String getPutString() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		// Cannot use longs since, guess what, JavaScript will round them. ;)
		builder.add(CustomerKeys.FULL_NAME, Names.getRandomName());
		builder.add(CustomerKeys.PHONE_NUMBER, Phones.getRandomPhone());
		return builder.build().toString();
	}

	private String putAndRemoveRandomCustomerAsync() {
		final String putString = getPutString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/rest/customers"))
				.timeout(Duration.ofMinutes(1)).header("Content-Type", "application/json")
				.PUT(BodyPublishers.ofString(putString)).build();
		client.sendAsync(request, BodyHandlers.ofString()).thenApply(HttpResponse::body)
				.thenAccept(this::onCustomerCreate);
		return putString;
	}

	@SuppressWarnings("unused")
	private String putAndRemoveRandomCustomer() throws IOException, InterruptedException {
		final String putString = getPutString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/rest/customers"))
				.timeout(Duration.ofMinutes(1)).header("Content-Type", "application/json")
				.PUT(BodyPublishers.ofString(putString)).build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		onCustomerCreate(response.body());
		return putString;
	}

	private void deleteCustomer(Customer customer) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest
				.newBuilder(URI.create("http://localhost:8080/rest/customers" + "/" + customer.getId()))
				.timeout(Duration.ofMinutes(1)).header("Content-Type", "application/json").DELETE().build();
		// Taking these synchronously...
		HttpResponse<String> response = deleteClient.send(request, BodyHandlers.ofString());
		onCustomerDelete(response.statusCode());
	}

	private void onCustomerCreate(String json) {
		Customer customer = Customer.fromJSon(json);
		System.out.println("Created customer: " + customer);
		System.out.println("Now deleting customer: " + customer);
		try {
			deleteCustomer(customer);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void onCustomerDelete(int statusCode) {
		System.out.println("Deleted customer. Status code: " + statusCode);
	}
}
