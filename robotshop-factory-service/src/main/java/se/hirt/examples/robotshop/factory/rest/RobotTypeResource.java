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
package se.hirt.examples.robotshop.factory.rest;

import javax.json.JsonObject;
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

import se.hirt.examples.robotshop.common.data.RobotType;
import se.hirt.examples.robotshop.factory.data.DataAccess;

/**
 * Resource for individual robot types.
 * 
 * @author Marcus Hirt
 */
public class RobotTypeResource {

	private final UriInfo uriInfo;
	private final String robotTypeId;
	private final RobotType robotType;

	public RobotTypeResource(UriInfo uriInfo, String id) {
		this.uriInfo = uriInfo;
		this.robotTypeId = id;
		this.robotType = DataAccess.getRobotTypeById(id);
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public String getRobotTypeId() {
		return robotTypeId;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getRobotType() {
		if (robotType == null) {
			throw new NotFoundException("robotType with id " + robotTypeId + " does not exist!");
		}
		return robotType.toJSon().build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putRobotType(JsonObject jsonEntity) {
		RobotType newType = RobotType.fromJSon(jsonEntity);

		if ((newType.getTypeId() != null) && !newType.getTypeId().equals(robotTypeId)) {
			return Response.status(409).entity("customerIds differ!\n").build();
		}
		DataAccess.addRobotType(newType);
		return Response.noContent().build();
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteRobotType() {
		if (robotType == null) {
			throw new NotFoundException("robotTypeId " + robotTypeId + "does not exist!");
		}
		DataAccess.removeRobotType(robotType);
		return Response.status(Status.OK).entity(robotType).build();
	}

}
