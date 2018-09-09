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

import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import se.hirt.examples.robotshop.order.OrderManager;
import se.hirt.examples.robotshop.order.data.RealizedOrder;

/**
 * Resource for an order that is ready for pickup.
 * 
 * @author Marcus Hirt
 */
public class RealizedOrderResource {
	private final UriInfo uriInfo;
	private final Long robotOrderId;
	private final RealizedOrder realizedOrder;

	public RealizedOrderResource(UriInfo uriInfo, Long id) {
		this.uriInfo = uriInfo;
		this.robotOrderId = id;
		this.realizedOrder = OrderManager.getInstance().getRealizedOrderById(id);
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public Long getRobotTypeId() {
		return robotOrderId;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getRobotType() {
		if (realizedOrder == null) {
			throw new NotFoundException("robotOrder with id " + robotOrderId + " does not exist!");
		}
		return realizedOrder.toJSon().build();
	}
}
