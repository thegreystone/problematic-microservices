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
package se.hirt.examples.robotshop.factory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import se.hirt.examples.robotshop.common.data.Color;
import se.hirt.examples.robotshop.common.data.Robot;
import se.hirt.examples.robotshop.common.util.Logger;
import se.hirt.examples.robotshop.common.util.Utils;

/**
 * Factory producing robots.
 * 
 * @author Marcus Hirt
 */
public class Factory {
	private final static Factory INSTANCE = new Factory();
	private final static int DEFAULT_NUMBER_OF_PRODUCTION_LINES = 4;
	private final static int JOB_QUEUE_SIZE = 500;
	private final static AtomicLong SERIAL_ID_GENERATOR = new AtomicLong();

	private final BlockingQueue<Runnable> jobQueue = new ArrayBlockingQueue<>(JOB_QUEUE_SIZE);
	private final Map<Long, Robot> completedRobots = new ConcurrentHashMap<>();
	private final Executor factoryLines = new ThreadPoolExecutor(0, DEFAULT_NUMBER_OF_PRODUCTION_LINES, 60,
			TimeUnit.SECONDS, jobQueue);

	private class ProductionJob implements Runnable {
		private long serialNumber;
		private String robotTypeId;
		private Color paint;

		public ProductionJob(long serialNumber, String robotTypeId, Color paint) {
			this.serialNumber = serialNumber;
			this.robotTypeId = robotTypeId;
			this.paint = paint;
		}

		@Override
		public void run() {
			Robot chassis = createChassis(serialNumber, robotTypeId);
			// Takes some time to roll the robot over to the painting
			Utils.sleep(10);
			Robot paintedRobot = paintRobot(chassis, paint);
			completedRobots.put(paintedRobot.getSerialNumber(), paintedRobot);
		}
	}

	/**
	 * Starts the production of a robot of the specified type.
	 * 
	 * @param robotTypeId
	 *            the type of robot to start building.
	 * @param paint
	 *            the color of the robot.
	 * @return the serial number of the robot to be produced.
	 * @throws RejectedExecutionException
	 *             if factory is too busy.
	 */
	public long startBuildingRobot(final String robotTypeId, final Color paint) throws RejectedExecutionException {
		final long serialNumber = SERIAL_ID_GENERATOR.getAndIncrement();
		startProduction(serialNumber, robotTypeId, paint);
		return serialNumber;
	}

	/**
	 * @return the robots available for pick-up.
	 */
	public Collection<Robot> getCompletedRobots() {
		return completedRobots.values();
	}

	/**
	 * @param robot
	 *            the robot to pick up.
	 * @return null, if there was no such robot to pick up from the factory yet (not completed), or
	 *         the completed robot.
	 */
	public Robot pickUp(long serialNumber) {
		return completedRobots.remove(serialNumber);
	}

	private void startProduction(long serialNumber, String robotTypeId, Color paint) {
		factoryLines.execute(new ProductionJob(serialNumber, robotTypeId, paint));
	}

	private static Robot paintRobot(Robot robotToPaint, Color paint) {
		Logger.log("Painting robot!");
		// Takes 20 ms to paint a robot. Yep, it's a kick ass robot factory.
		Utils.sleep(50);
		return new Robot(robotToPaint.getSerialNumber(), robotToPaint.getRobotType(), paint);
	}

	private static Robot createChassis(long serialNumber, String robotTypeId) {
		Logger.log("Creating robot chassis!");
		// Takes 30 ms to create a robot chassis. Yep, it's a kick ass robot factory.
		Utils.sleep(50);
		return new Robot(serialNumber, robotTypeId, null);
	}

	public static Factory getInstance() {
		return INSTANCE;
	}
}
