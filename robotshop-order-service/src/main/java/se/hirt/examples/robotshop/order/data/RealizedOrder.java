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
package se.hirt.examples.robotshop.order.data;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import se.hirt.examples.robotshop.common.util.Utils;
import se.hirt.examples.robotshop.common.data.Customer;
import se.hirt.examples.robotshop.common.data.Robot;

/**
 * An order that is ready for delivery. Contains the orderId and all the ordered robots.
 * 
 * @author Marcus Hirt
 */
public class RealizedOrder {
	public final static String KEY_ROBOTS = "robots";
	public final static String KEY_CUSTOMER = "customer";
	public final static String KEY_ORDER = "order";
	private final RobotOrder order;
	private final Customer customer;
	private final Robot[] robots;
	private final Throwable error;

	public RealizedOrder(RobotOrder order, Customer customer, Robot[] robots, Throwable error) {
		this.order = order;
		this.customer = customer;
		this.robots = robots;
		this.error = error;
	}

	public RobotOrder getOrder() {
		return order;
	}

	public Customer getCustomer() {
		return customer;
	}

	public Robot[] getRobots() {
		return robots;
	}

	public Throwable getError() {
		return error;
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(KEY_CUSTOMER, customer.toJSon());
		builder.add(KEY_ORDER, order.toJSon());
		builder.add(KEY_ROBOTS, serializeRobotsToJSon());
		if (error != null) {
			builder.add(Utils.KEY_ERROR, Utils.errorAsJSon(error));
		}
		return builder;
	}

	private JsonArrayBuilder serializeRobotsToJSon() {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Robot robot : robots) {
			builder.add(robot.toJSon());
		}
		return builder;
	}
}
