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
package se.hirt.examples.robotfactory.rest;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import se.hirt.examples.robotfactory.Factory;
import se.hirt.examples.robotfactory.data.Color;
import se.hirt.examples.robotfactory.data.Robot;
import se.hirt.examples.robotfactory.data.RobotType;
import se.hirt.examples.robotfactory.utils.Utils;

/**
 * API for interacting with a factory producing robots.
 * 
 * @author Marcus Hirt
 */
@Path("/factory/")
public class FactoryResource {
	@GET
	@Path("/completed/")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray listCompleted() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (Robot robot : Factory.getInstance().getCompletedRobots()) {
			arrayBuilder.add(robot.toJSon());
		}
		return arrayBuilder.build();
	}

	@POST
	@Path("/buildrobot")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buildRobot(
		@QueryParam(RobotType.KEY_ROBOT_TYPE) String robotTypeId, @QueryParam(Robot.KEY_COLOR) String color) {
		JsonObjectBuilder createObjectBuilder = Json.createObjectBuilder();

		if (robotTypeId == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Utils.errorAsJSonString(RobotType.KEY_ROBOT_TYPE + " must not be null!")).build();
		}

		if (color == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Utils.errorAsJSonString(Robot.KEY_COLOR + " must not be null!")).build();
		}

		Color paint = Color.fromString(color);
		if (paint == null) {
			return Response.status(Status.BAD_REQUEST).entity(Utils.errorAsJSonString(color + " is not a valid color!"))
					.build();
		}

		long serialNumber = Factory.getInstance().startBuildingRobot(robotTypeId, paint);
		createObjectBuilder.add(Robot.KEY_SERIAL_NUMBER, serialNumber);
		return Response.accepted(createObjectBuilder.build()).build();
	}

	@GET
	@Path("/pickup")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buildRobot(@QueryParam(Robot.KEY_SERIAL_NUMBER) Long serialNumber) {
		if (serialNumber == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Utils.errorAsJSonString(Robot.KEY_SERIAL_NUMBER + " must not be null!")).build();
		}
		Robot robot = Factory.getInstance().pickUp(serialNumber);
		if (robot == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		return Response.ok(robot.toJSon().build()).build();
	}
}
