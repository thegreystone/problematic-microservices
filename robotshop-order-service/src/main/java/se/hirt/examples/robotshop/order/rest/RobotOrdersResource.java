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
package se.hirt.examples.robotshop.order.rest;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import se.hirt.examples.robotshop.common.data.Customer;
import se.hirt.examples.robotshop.common.data.RobotOrder;
import se.hirt.examples.robotshop.common.data.RobotOrderLineItem;
import se.hirt.examples.robotshop.common.opentracing.OpenTracingFilter;
import se.hirt.examples.robotshop.common.util.Utils;
import se.hirt.examples.robotshop.order.OrderManager;

/**
 * Rest API for orders not yet fulfilled.
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
		for (RobotOrder order : OrderManager.getInstance().getActiveOrders()) {
			arrayBuilder.add(order.toJSon());
		}
		return arrayBuilder.build();
	}

	@Path("{robotOrderId}/")
	public RobotOrderResource getOrder(@PathParam(RobotOrder.KEY_ORDER_ID) Long robotOrderId) {
		return new RobotOrderResource(uriInfo, robotOrderId);
	}

	@POST
	@Path("/new")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createNewOrder(@Context HttpServletRequest request, JsonObject jsonEntity) {
		String customerIdStr = jsonEntity.getString(Customer.KEY_CUSTOMER_ID);
		if (customerIdStr == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Utils.errorAsJSonString("Request did not specify customer!")).build();
		}
		Long customerId = Long.parseLong(customerIdStr);
		JsonValue jsonValue = jsonEntity.get(RobotOrder.KEY_LINE_ITEMS);
		List<RobotOrderLineItem> lineItems = jsonValue.asJsonArray().stream().map(RobotOrderLineItem::fromJSon)
				.collect(Collectors.toList());
		try {
			RobotOrder newOrder = OrderManager.getInstance().createNewOrder(customerId,
					lineItems.toArray(new RobotOrderLineItem[0]));
			OrderManager.getInstance().dispatchOrder(newOrder, OpenTracingFilter.getActiveContext(request));
			return Response.accepted(newOrder.toJSon().build()).build();
		} catch (RejectedExecutionException e) {
			return Response.status(Status.SERVICE_UNAVAILABLE)
					.entity(Utils.errorAsJSonString("Order Service is overworked!")).build();
		}
	}
}
