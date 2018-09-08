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
package se.hirt.examples.customerservice.rest;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import se.hirt.examples.customerservice.data.Customer;
import se.hirt.examples.customerservice.data.DataAccess;

/**
 * Resource for individual customers.
 * 
 * @author Marcus Hirt
 */
public class CustomerResource {
	private final UriInfo uriInfo;
	private final String id;
	private final Customer customer;

	public CustomerResource(UriInfo uriInfo, String id) {
		this.uriInfo = uriInfo;
		this.id = id;
		this.customer = DataAccess.getCustomerById(Long.valueOf(id));
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public String getCustomerId() {
		return id;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getCustomer() {
		if (null == customer) {
			throw new NotFoundException("customerId " + id + " does not exist!");
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		// Cannot use longs since, guess what, JavaScript will round them. ;)
		builder.add(Customer.KEY_CUSTOMER_ID, String.valueOf(customer.getId()));
		builder.add(Customer.KEY_FULL_NAME, customer.getFullName());
		builder.add(Customer.KEY_PHONE_NUMBER, customer.getPhoneNumber());
		return builder.build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putUser(JsonObject jsonEntity) {
		String jsonId = jsonEntity.getString(Customer.KEY_CUSTOMER_ID);

		if ((jsonId != null) && !jsonId.equals(id)) {
			return Response.status(409).entity("customerIds differ!\n").build();
		}

		// If we have no customer, this is an insert, otherwise an update
		final boolean newRecord = (null == customer);

		String fullName = jsonEntity.getString(Customer.KEY_FULL_NAME);
		String phoneNumber = jsonEntity.getString(Customer.KEY_PHONE_NUMBER);

		if (newRecord) {
			// We're allowing inserts here, but ID will be generated (i.e. we will ignore 
			// the ID provided by the path)
			DataAccess.createCustomer(fullName, phoneNumber);
			return Response.created(uriInfo.getAbsolutePath()).build();
		} else {
			DataAccess.updateCustomer(Long.valueOf(jsonId), fullName, phoneNumber);
			return Response.noContent().build();
		}
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteUser() {
		if (customer == null) {
			throw new NotFoundException("customerId " + id + "does not exist!");
		}
		DataAccess.removeCustomer(customer);
		return Response.status(Status.OK).entity(customer).build();
	}

}
