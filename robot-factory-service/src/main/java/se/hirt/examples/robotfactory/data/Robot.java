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
package se.hirt.examples.robotfactory.data;

import java.io.Serializable;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * A robot, serial number and all.
 * 
 * @author Marcus Hirt
 */
public class Robot implements Serializable {
	private static final long serialVersionUID = -7669748978172006987L;
	public final static String KEY_SERIAL_NUMBER = "serialNumber";
	public final static Long INVALID_SERIAL_ID = -1L;
	public static final String KEY_COLOR = "color";

	private final long serialNumber;
	private final String robotType;
	private final Color color;

	public Robot(long serialNumber, String robotType, Color color) {
		this.serialNumber = serialNumber;
		this.robotType = robotType;
		this.color = color;
	}

	public long getSerialNumber() {
		return serialNumber;
	}

	public String getRobotType() {
		return robotType;
	}

	public Color getColor() {
		return color;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (serialNumber ^ (serialNumber >>> 32));
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
		Robot other = (Robot) obj;
		if (serialNumber != other.serialNumber)
			return false;
		return true;
	}

	public String toString() {
		return String.format("Robot [serialNo=%d, type=%s]", serialNumber, robotType);
	}

	public static Robot fromJSon(String jsonString) {
		return fromJSon(Json.createReader(new StringReader(jsonString)).readObject());
	}

	public static Robot fromJSon(JsonObject json) {
		String serialNumberStr = json.getString(KEY_SERIAL_NUMBER);

		if ((serialNumberStr == null)) {
			throw new IllegalArgumentException("Must have a serial number to create robot object from JSon");
		}

		long serial = Long.parseLong(serialNumberStr);
		String type = json.getString(RobotType.KEY_ROBOT_TYPE);
		Color color = Color.fromString(json.getString(KEY_COLOR));
		return new Robot(Long.valueOf(serial), type, color);
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		// Cannot use longs since, guess what, JavaScript will round them. ;)
		builder.add(KEY_SERIAL_NUMBER, String.valueOf(getSerialNumber()));
		builder.add(RobotType.KEY_ROBOT_TYPE, getRobotType());
		builder.add(KEY_COLOR, getColor().toString());
		return builder;
	}

}
