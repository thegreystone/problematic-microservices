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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.validation.ValidationException;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.contrib.okhttp3.TracingCallFactory;
import io.opentracing.util.GlobalTracer;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import se.hirt.examples.robotshop.common.data.Color;
import se.hirt.examples.robotshop.common.data.Customer;
import se.hirt.examples.robotshop.common.data.RealizedOrder;
import se.hirt.examples.robotshop.common.data.Robot;
import se.hirt.examples.robotshop.common.data.RobotOrder;
import se.hirt.examples.robotshop.common.data.RobotOrderLineItem;
import se.hirt.examples.robotshop.common.data.RobotType;
import se.hirt.examples.robotshop.common.opentracing.SpanDecorator;
import se.hirt.examples.robotshop.common.util.Logger;

/**
 * The thing actually generating some load.
 * 
 * @author Marcus Hirt
 */
public class LoadWorker implements Runnable {
	private final static Random RND = new Random();

	private final static MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");

	private final static ScheduledExecutorService COMPLETION_POLL_EXECUTOR = Executors.newScheduledThreadPool(4);;
	private final Call.Factory httpClient = new TracingCallFactory(new OkHttpClient(), GlobalTracer.get(),
			SpanDecorator.getSpanDecorators());

	private final String urlCustomer;
	private final String urlFactory;
	private final String urlOrder;
	private final int minRobotsPerOrder;
	private final int maxRobotsPerOrder;

	public LoadWorker(Properties configuration) {
		urlCustomer = configuration.getProperty("urlCustomerService");
		urlFactory = configuration.getProperty("urlFactoryService");
		urlOrder = configuration.getProperty("urlOrderService");
		minRobotsPerOrder = Integer.parseInt(configuration.getProperty("minRobotsPerOrder", "2"));
		maxRobotsPerOrder = Integer.parseInt(configuration.getProperty("maxRobotsPerOrder", "10"));
		validate();
	}

	private void validate() {
		if (urlCustomer == null) {
			throw new ValidationException("No URL for the Customer Service!");
		}
		if (urlFactory == null) {
			throw new ValidationException("No URL for the Factory Service!");
		}
		if (urlOrder == null) {
			throw new ValidationException("No URL for the Factory Service!");
		}
	}

	@Override
	public void run() {
		try {
			doFullRegisterOrderAndRemove();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void doFullRegisterOrderAndRemove() throws InterruptedException, ExecutionException {
		SpanBuilder builder = getTracer().buildSpan("fullSystemTest");
		try (Scope scope = builder.startActive(true)) {
			SpanContext parent = scope.span().context();
			Customer c = registerRandomCustomer(scope.span().context());

			// Maybe not get these guys over and over, looks pretty in the traces though...
			CompletableFuture<Customer> newCustomer = CompletableFuture
					.supplyAsync(() -> registerRandomCustomer(parent));
			CompletableFuture<RobotType[]> availableTypes = CompletableFuture.supplyAsync(() -> getAllTypes(parent));
			CompletableFuture<Color[]> availableColors = CompletableFuture.supplyAsync(() -> getAllColors(parent));
			CompletableFuture.allOf(newCustomer, availableTypes, availableColors);

			// First completion stage done. Now we can create the order			
			List<RobotOrderLineItem> lineItems = createRandomOrder(availableTypes.get(), availableColors.get());
			CompletableFuture<RobotOrder> robotOrderCompletable = CompletableFuture
					.supplyAsync(() -> postOrder(c, lineItems, parent));

			// Rest will happen asynchrously when data is available...
			CompletableFuture<RealizedOrder> realizedOrderFuture = new CompletableFuture<RealizedOrder>();
			// When we have the order, we schedule the polling for an available order...
			robotOrderCompletable.thenAccept((order) -> awaitOrderCompletion(order, realizedOrderFuture, parent));
			// Once the order is realized, we will remove the customer.
			realizedOrderFuture.thenApply((realizedOrder) -> removeOwner(realizedOrder, parent));
		}
	}

	private String removeOwner(RealizedOrder realizedOrder, SpanContext ctx) {
		System.out.println("User " + realizedOrder.getCustomer() + " picked up " + realizedOrder.getOrder().getOrderId() + ". Now removing customer.");

		Customer customer = realizedOrder.getCustomer();
		String url = urlCustomer + "/customers/" + customer.getId();
		Request request = new Request.Builder().url(url).delete().build();

		SpanBuilder spanBuilder = getTracer().buildSpan("DELETE: " + url);
		spanBuilder.addReference(References.FOLLOWS_FROM, ctx);

		try (Scope scope = spanBuilder.startActive(true)) {
			Response response = httpClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				Logger.log("Failed to call DELETE:" + url);
				return null;
			}
			System.out.println("User " + realizedOrder.getCustomer() + " removed.");
			// FIXME: Get from return value
			return String.valueOf(customer.getId());
		} catch (IOException e) {
			Logger.log(e.getMessage());
		}
		return null;
	}

	private void awaitOrderCompletion(RobotOrder order, CompletableFuture<RealizedOrder> future, SpanContext parent) {
		System.out.println("Created order " + order.getOrderId() + " for user " + order.getCustomerId()
				+ ", now awaiting completion.");
		OrderCompletionMonitor job = new OrderCompletionMonitor(urlOrder + "/readyorders/pickup", order, future,
				parent);
		job.scheduledFuture = COMPLETION_POLL_EXECUTOR.scheduleAtFixedRate(job, 50, 2000, TimeUnit.MILLISECONDS);
	}

	private RobotOrder postOrder(Customer c, List<RobotOrderLineItem> lineItems, SpanContext parent) {
		String url = urlOrder + "/orders/new";
		JsonObjectBuilder jsonBodyBuilder = Json.createObjectBuilder();
		jsonBodyBuilder.add(Customer.KEY_CUSTOMER_ID, String.valueOf(c.getId()));

		JsonArrayBuilder lineItemBuilder = Json.createArrayBuilder();
		for (RobotOrderLineItem lineItem : lineItems) {
			lineItemBuilder.add(lineItem.toJSon());
		}

		jsonBodyBuilder.add(RobotOrder.KEY_LINE_ITEMS, lineItemBuilder);
		String bodyStr = jsonBodyBuilder.build().toString();
		RequestBody body = RequestBody.create(JSON, bodyStr);

		Request request = new Request.Builder().url(url).post(body).build();

		SpanBuilder spanBuilder = getTracer().buildSpan("POST: " + url);
		spanBuilder.asChildOf(parent);

		try (Scope scope = spanBuilder.startActive(true)) {
			Response response = httpClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				Logger.log("Failed to call POST:" + url);
				Logger.log("Response: " + response.body().string());
				return null;
			}
			return RobotOrder.fromJSon(response.body().string());
		} catch (IOException e) {
			Logger.log(e.getMessage());
		}
		return null;
	}

	private List<RobotOrderLineItem> createRandomOrder(RobotType[] robotTypes, Color[] colors) {
		int robotCount = RND.nextInt(maxRobotsPerOrder - minRobotsPerOrder) + 1;

		List<RobotOrderLineItem> lineItems = new ArrayList<>(robotCount);

		for (int i = 0; i < robotCount; i++) {
			lineItems.add(createRandomLineItem(robotTypes, colors));
		}
		return lineItems;
	}

	private RobotOrderLineItem createRandomLineItem(RobotType[] robotTypes, Color[] colors) {
		return new RobotOrderLineItem(robotTypes[RND.nextInt(robotTypes.length)], colors[RND.nextInt(colors.length)]);
	}

	private Color[] getAllColors(SpanContext parent) {
		List<Color> paints = new ArrayList<Color>();
		String result = doGeneralGetCall(urlFactory + "/paints", parent, References.CHILD_OF);
		JsonReader reader = Json.createReader(new StringReader(result));
		JsonArray array = reader.readArray();
		for (JsonValue jsonValue : array) {
			paints.add(Color.fromString(jsonValue.asJsonObject().getString(Robot.KEY_COLOR)));
		}
		return paints.toArray(new Color[0]);
	}

	private RobotType[] getAllTypes(SpanContext parent) {
		List<RobotType> types = new ArrayList<RobotType>();
		String result = doGeneralGetCall(urlFactory + "/robottypes", parent, References.CHILD_OF);
		JsonReader reader = Json.createReader(new StringReader(result));
		JsonArray array = reader.readArray();
		for (JsonValue jsonValue : array) {
			types.add(RobotType.fromJSon(jsonValue.asJsonObject()));
		}
		return types.toArray(new RobotType[0]);
	}

	private String doGeneralGetCall(String url, SpanContext parent, String kindOfSpanReference) {
		Request request = new Request.Builder().url(url).get().build();

		SpanBuilder spanBuilder = getTracer().buildSpan("GET: " + url);
		spanBuilder.addReference(kindOfSpanReference, parent);

		try (Scope scope = spanBuilder.startActive(true)) {
			Response response = httpClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				Logger.log("Failed to call GET:" + url);
				return null;
			}
			return response.body().string();
		} catch (IOException e) {
			Logger.log(e.getMessage());
		}
		return null;
	}

	private Customer registerRandomCustomer(SpanContext parent) {
		String newCustomerJSon = getNewCustomerJSonString();
		RequestBody body = RequestBody.create(JSON, newCustomerJSon);
		Request request = new Request.Builder().url(urlCustomer + "/customers").put(body).build();

		SpanBuilder spanBuilder = getTracer().buildSpan("Create random user");
		spanBuilder.asChildOf(parent);

		try (Scope scope = spanBuilder.startActive(true)) {
			Response response = httpClient.newCall(request).execute();
			if (!response.isSuccessful()) {
				Logger.log("Failed to create customer " + newCustomerJSon);
				return null;
			}
			return Customer.fromJSon(response.body().string());
		} catch (IOException e) {
			Logger.log(e.getMessage());
		}
		return null;
	}

	private static Tracer getTracer() {
		return GlobalTracer.get();
	}

	private String getNewCustomerJSonString() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(Customer.KEY_FULL_NAME, Names.getRandomName());
		builder.add(Customer.KEY_PHONE_NUMBER, Phones.getRandomPhone());
		return builder.build().toString();
	}

	public static void main(String[] args) throws IOException {
		byte [] input = new byte[5];
		LoadWorker worker = new LoadWorker(Utils.loadPropertiesFromResource("load.properties"));
		while (input[0] != 'q') {
			worker.run();
			System.out.println("Press <enter> to continue, or q and <enter to quit>!\n");
			System.in.read(input);
		}
		COMPLETION_POLL_EXECUTOR.shutdown();
	}
}
