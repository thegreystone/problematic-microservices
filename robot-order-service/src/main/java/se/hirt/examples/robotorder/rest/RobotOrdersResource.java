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
package se.hirt.examples.robotorder.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.validation.ValidationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import se.hirt.examples.customerservice.data.Customer;
import se.hirt.examples.robotfactory.utils.Utils;
import se.hirt.examples.robotorder.OrderManager;
import se.hirt.examples.robotorder.RealizedOrder;
import se.hirt.examples.robotorder.data.DataAccess;
import se.hirt.examples.robotorder.data.RobotOrder;
import se.hirt.examples.robotorder.data.RobotOrderLineItem;

/**
 * Rest API for customers.
 * 
 * @author Marcus Hirt
 */
@Path("/orders/")
public class RobotOrdersResource {
	@Context
	UriInfo uriInfo;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray list() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (RobotOrder order : DataAccess.getAvailableRobotOrders()) {
			arrayBuilder.add(order.toJSon());
		}
		return arrayBuilder.build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putRobotOrder(JsonObject jsonEntity) {
		try {
			RobotOrder robotOrder = RobotOrder.fromJSon(jsonEntity);
			DataAccess.addRobotOrder(robotOrder);
			return Response.accepted(robotOrder.toJSon().build().toString()).build();
		} catch (ValidationException e) {
			return Response.status(Status.BAD_REQUEST).entity(Utils.errorAsJSonString(e)).build();
		}
	}

	@Path("{robotOrderId}/")
	public RobotOrderResource getOrder(@PathParam(RobotOrder.KEY_ORDER_ID) Long robotOrderId) {
		return new RobotOrderResource(uriInfo, robotOrderId);
	}

	@POST
	@Path("/new")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createNewOrder(JsonObject jsonEntity) {
		JsonNumber number = jsonEntity.getJsonNumber(Customer.KEY_CUSTOMER_ID);
		if (number == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Utils.errorAsJSonString("Request did not specify customer!")).build();
		}
		Long customerId = number.longValueExact();
		JsonValue jsonValue = jsonEntity.get(RobotOrder.KEY_LINE_ITEMS);
		List<RobotOrderLineItem> lineItems = jsonValue.asJsonArray().stream().map(RobotOrderLineItem::fromJSon)
				.collect(Collectors.toList());
		RobotOrder newOrder = OrderManager.getInstance().createNewOrder(customerId,
				lineItems.toArray(new RobotOrderLineItem[0]));
		OrderManager.getInstance().dispatchOrder(newOrder);
		return Response.accepted(newOrder).build();
	}

	@GET
	@Path("/pickup")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buildRobot(@QueryParam(RobotOrder.KEY_ORDER_ID) Long orderId) {
		if (orderId == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Utils.errorAsJSonString(RobotOrder.KEY_ORDER_ID + " must not be null!")).build();
		}
		RealizedOrder robotOrder = OrderManager.getInstance().pickUpOrder(orderId);
		if (robotOrder == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		return Response.ok(robotOrder.toJSon().build()).build();
	}
}
