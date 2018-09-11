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
package se.hirt.examples.robotshop.common.data;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import se.hirt.examples.robotshop.common.data.Color;
import se.hirt.examples.robotshop.common.data.Robot;
import se.hirt.examples.robotshop.common.data.RobotType;

/**
 * A line item in a purchase order for robots.
 * 
 * @author Marcus Hirt
 */
public final class RobotOrderLineItem {
	private final String robotTypeId;
	private final Color color;

	public RobotOrderLineItem(String robotTypeId, Color color) {
		this.robotTypeId = robotTypeId;
		this.color = color;
	}

	public RobotOrderLineItem(RobotType robotType, Color color) {
		this(robotType.getTypeId(), color);
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(RobotType.KEY_ROBOT_TYPE, robotTypeId);
		builder.add(Robot.KEY_COLOR, color.toString());
		return builder;
	}

	public static RobotOrderLineItem fromJSon(JsonObject json) {
		String type = json.getString(RobotType.KEY_ROBOT_TYPE);
		Color color = Color.fromString(json.getString(Robot.KEY_COLOR));
		return new RobotOrderLineItem(type, color);
	}

	public static RobotOrderLineItem fromJSon(JsonValue jsonValue) {
		return fromJSon(jsonValue.asJsonObject());
	}

	public String getRobotTypeId() {
		return robotTypeId;
	}

	public Color getColor() {
		return color;
	}

	public String toString() {
		return String.format("LineItem [%s:%s, %s:%s]", RobotType.KEY_ROBOT_TYPE, getRobotTypeId(), Robot.KEY_COLOR,
				getColor());
	}
}
