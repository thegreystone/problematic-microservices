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
package se.hirt.examples.robotorder.data;

import java.io.Serializable;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import se.hirt.examples.customerservice.data.Customer;

/**
 * An order for purchasing robots.
 * 
 * @author Marcus Hirt
 */
public class RobotOrder implements Serializable {
	private static final long serialVersionUID = -8387175138530273342L;

	public static final String KEY_ORDER_ID = "orderId";
	public static final String KEY_PLACEMENT_TIME = "placementTime";
	public static final String KEY_LINE_ITEMS = "lineItems";

	private final Long orderId;
	private final Long customerId;
	private final ZonedDateTime placementTime;
	private final RobotOrderLineItem[] lineItems;

	/**
	 * Constructor.
	 * 
	 * @param orderId
	 *            the id of the order.
	 * @param placementTime
	 *            the time the order was placed.
	 * @param lineItems
	 *            the line items in the order. May not be null.
	 * @throws NullPointerException
	 *             if lineItems is null.
	 */
	public RobotOrder(long orderId, long customerId, ZonedDateTime placementTime, RobotOrderLineItem[] lineItems) {
		this.orderId = orderId;
		this.customerId = customerId;
		this.placementTime = placementTime;
		this.lineItems = lineItems;
		if (lineItems == null) {
			throw new NullPointerException("An order must have line items");
		}
	}

	public Long getOrderId() {
		return orderId;
	}

	public ZonedDateTime getPlacementTime() {
		return placementTime;
	}

	public RobotOrderLineItem[] getLineItems() {
		return lineItems;
	}

	public String toString() {
		return String.format("RobotOrder [orderId=%d, placementTime=%s, #lineItems=%d]", orderId,
				placementTime.toString(), lineItems.length);
	}

	public static RobotOrder fromJSon(String jsonString) {
		return fromJSon(Json.createReader(new StringReader(jsonString)).readObject());
	}

	public static RobotOrder fromJSon(JsonObject json) {
		String jsonOrderIdStr = json.getString(KEY_ORDER_ID);
		if ((jsonOrderIdStr == null)) {
			throw new IllegalArgumentException("Must have an orderId to create an order");
		}

		JsonNumber jsonCustomerId = json.getJsonNumber(Customer.KEY_CUSTOMER_ID);
		if ((jsonCustomerId == null)) {
			throw new IllegalArgumentException("Must have an customerId to create an order");
		}

		long orderId = Long.valueOf(jsonOrderIdStr);
		long customerId = jsonCustomerId.longValueExact();
		ZonedDateTime placementTime = ZonedDateTime.parse(json.getString(KEY_PLACEMENT_TIME));
		RobotOrderLineItem[] lineItems = parseLineItems(json.getJsonArray(KEY_LINE_ITEMS));

		return new RobotOrder(orderId, customerId, placementTime, lineItems);
	}

	private static RobotOrderLineItem[] parseLineItems(JsonArray jsonArray) {
		List<RobotOrderLineItem> items = jsonArray.stream().map(RobotOrderLineItem::fromJSon)
				.collect(Collectors.toList());
		return items.toArray(new RobotOrderLineItem[0]);
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		// Cannot use longs since, guess what, JavaScript will round them. ;)
		builder.add(KEY_ORDER_ID, String.valueOf(orderId));
		builder.add(KEY_PLACEMENT_TIME, placementTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

		JsonArrayBuilder lineItemBuilder = Json.createArrayBuilder();
		for (RobotOrderLineItem lineItem : lineItems) {
			lineItemBuilder.add(lineItem.toJSon());
		}

		builder.add(KEY_LINE_ITEMS, lineItemBuilder);
		return builder;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (orderId ^ (orderId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RobotOrder other = (RobotOrder) obj;
		if (orderId != other.orderId)
			return false;
		return true;
	}

	public Long getCustomerId() {
		return customerId;
	}
}
