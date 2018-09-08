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
 * Description of a type of robot. Handy when ordering one.
 * 
 * @author Marcus Hirt
 */
public final class RobotType implements Serializable {
	private static final long serialVersionUID = -3012533094235973369L;
	public final static String KEY_ROBOT_TYPE = "robotTypeId";
	public final static String KEY_ROBOT_DESCRIPTION = "description";

	private final String typeId;
	private final String description;

	RobotType(String typeId, String description) {
		this.typeId = typeId;
		this.description = description;
	}

	public String getTypeId() {
		return typeId;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		return String.format("RobotType [typeId=%d, description=%s]", typeId, description);
	}

	public static RobotType fromJSon(String jsonString) {
		return fromJSon(Json.createReader(new StringReader(jsonString)).readObject());
	}

	public static RobotType fromJSon(JsonObject json) {
		String typeId = json.getString(KEY_ROBOT_TYPE);

		if (typeId == null) {
			throw new IllegalArgumentException("Must have a typeId to create a robot type from JSon");
		}
		String description = json.getString(KEY_ROBOT_DESCRIPTION);
		return new RobotType(typeId, description);
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(KEY_ROBOT_TYPE, getTypeId());
		builder.add(KEY_ROBOT_DESCRIPTION, getDescription());
		return builder;
	}

}
