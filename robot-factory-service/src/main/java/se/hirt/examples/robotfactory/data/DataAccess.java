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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplified data access. FIXME: Store in couch base/cassandra/whatever:
 * 
 * @author Marcus Hirt
 */
public class DataAccess {
	private final static Map<String, RobotType> ROBOT_TYPES = new HashMap<>();

	static {
		addDefaultRobotTypes();
	}

	public static Collection<RobotType> getAvailableRobotTypes() {
		return ROBOT_TYPES.values();
	}

	private static void addDefaultRobotTypes() {
		createRobotType("Wall-E", "Cute little cubic garbage disposal robot.");
		createRobotType("EVE", "Advanced little robot. Specializes in retrieval of plants.");
		createRobotType("Coff-E", "3D printed robot with laser range finder. Looks a bit like Wall-E.");
		createRobotType("T-800", "Terminator robot. Please read the legal disclaimers and owner responsibilities.");
		createRobotType("T-1000",
				"Terminator robot that can change shape. Please read the legal disclaimers and owner responsibilities.");
		createRobotType("R2-D2", "Versatile astromech droid.");
		createRobotType("BB-8", "Spherical astromech droid.");
		createRobotType("Baymax", "Inflatable medical droid.");
	}

	private static void createRobotType(String typeId, String description) {
		addRobotType(new RobotType(typeId, description));
	}

	public static void addRobotType(RobotType robotType) {
		ROBOT_TYPES.put(robotType.getTypeId(), robotType);
	}

	public static RobotType getRobotTypeById(String id) {
		return ROBOT_TYPES.get(id);
	}

	public static void removeRobotType(RobotType robotType) {
		ROBOT_TYPES.remove(robotType.getTypeId());
	}
}
