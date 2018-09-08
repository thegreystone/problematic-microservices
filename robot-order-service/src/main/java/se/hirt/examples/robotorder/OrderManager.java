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
package se.hirt.examples.robotorder;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.ws.rs.core.Response.Status;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se.hirt.examples.customerservice.data.Customer;
import se.hirt.examples.customerservice.data.ValidationException;
import se.hirt.examples.robotfactory.data.Robot;
import se.hirt.examples.robotfactory.data.RobotType;
import se.hirt.examples.robotorder.data.RobotOrder;
import se.hirt.examples.robotorder.data.RobotOrderLineItem;

/**
 * Order manager, tracking and fulfilling robot orders.
 * 
 * @author Marcus Hirt
 */
public class OrderManager {
	private final static String CUSTOMER_SERVICE_LOCATION;
	private final static String ROBOT_FACTORY_SERVICE_LOCATION;

	private final static String DEFAULT_CUSTOMER_SERVICE_LOCATION = "http://localhost:8081";
	private final static String DEFAULT_ROBOT_FACTORY_SERVICE_LOCATION = "http://localhost:8082";

	private final static OrderManager INSTANCE = new OrderManager();
	private final static int DEFAULT_NUMBER_OF_ORDER_DISPATCHERS = 4;
	private final static AtomicLong SERIAL_ID_GENERATOR = new AtomicLong();

	private final Map<Long, RobotOrder> orderQueue = new ConcurrentHashMap<>();
	private final Map<Long, RealizedOrder> completedOrders = new ConcurrentHashMap<>();
	private final Executor orderDispatcher = new ThreadPoolExecutor(0, DEFAULT_NUMBER_OF_ORDER_DISPATCHERS, 60,
			TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	private final ScheduledExecutorService completionPollExecutor = Executors.newScheduledThreadPool(4);

	private final OkHttpClient httpClient = new OkHttpClient();

	static {
		// Setting up service locations...
		String robotFactory = System.getenv("ROBOT_FACTORY_SERVICE_LOCATION");
		if (robotFactory == null) {
			robotFactory = DEFAULT_ROBOT_FACTORY_SERVICE_LOCATION;
		}
		String customerService = System.getenv("CUSTOMER_SERVICE_LOCATION");
		if (customerService == null) {
			customerService = DEFAULT_CUSTOMER_SERVICE_LOCATION;
		}
		ROBOT_FACTORY_SERVICE_LOCATION = robotFactory;
		CUSTOMER_SERVICE_LOCATION = customerService;
	}

	private class RobotPickupJob implements Runnable {
		private final Long serial;
		private final CompletableFuture<Robot> future;
		public volatile ScheduledFuture<?> scheduledFuture;

		public RobotPickupJob(Long serial, CompletableFuture<Robot> future) {
			this.serial = serial;
			this.future = future;
		}

		@Override
		public void run() {
			FormBody.Builder formBuilder = new FormBody.Builder().add(Robot.KEY_SERIAL_NUMBER, String.valueOf(serial));
			Request req = new Request.Builder().url(ROBOT_FACTORY_SERVICE_LOCATION + "/factory/pickup")
					.post(formBuilder.build())
					//.tag(new TagWrapper(parentSpan.context()))
					.build();
			Response response;
			try {
				response = httpClient.newCall(req).execute();
				String body = response.body().string();
				if (!body.isEmpty()) {
					Robot robot = Robot.fromJSon(body);
					future.complete(robot);
					scheduledFuture.cancel(false);
				}
			} catch (IOException e) {
				e.printStackTrace();
				scheduledFuture.cancel(false);
			}
		}
	}

	private class RobotFactoryRequestSerialSupplier implements Supplier<Long> {
		private final RobotOrderLineItem lineItem;

		public RobotFactoryRequestSerialSupplier(RobotOrderLineItem lineItem) {
			this.lineItem = lineItem;
		}

		@Override
		public Long get() {
			FormBody.Builder formBuilder = new FormBody.Builder();
			formBuilder.add(RobotType.KEY_ROBOT_TYPE, lineItem.getRobotTypeId());
			formBuilder.add(Robot.KEY_COLOR, lineItem.getColor().toString());

			Request req = new Request.Builder().url(ROBOT_FACTORY_SERVICE_LOCATION + "/factory/buildrobot")
					.post(formBuilder.build())
					//.tag(new TagWrapper(parentSpan.context()))
					.build();

			try {
				Response response = httpClient.newCall(req).execute();
				return parseSerial(response.body().string());
			} catch (Throwable ioe) {
				ioe.printStackTrace();
			}
			return -1L;
		}

		private Long parseSerial(String json) {
			JsonObject readObject = Json.createReader(new StringReader(json)).readObject();
			JsonNumber jsonNumber = readObject.asJsonObject().getJsonNumber(Robot.KEY_SERIAL_NUMBER);
			if (jsonNumber == null) {
				return Robot.INVALID_SERIAL_ID;
			}
			return jsonNumber.longValueExact();
		}
	}

	private class OrderJob implements Runnable {
		private RobotOrder order;

		public OrderJob(RobotOrder order) {
			this.order = order;
		}

		@Override
		public void run() {
			try {
				Customer customer = validateUser(order.getCustomerId());
				Collection<CompletableFuture<Robot>> robots = dispatch(order.getLineItems());
				CompletableFuture<Void> allOf = CompletableFuture.allOf(robots.toArray(new CompletableFuture[0]));
				allOf.get();
				List<Robot> collect = robots.stream().map((robot) -> get(robot)).collect(Collectors.toList());

				// TODO verify that all list items got realized - otherwise add errors for the ones missing etc
				completedOrders.put(order.getOrderId(),
						new RealizedOrder(order, customer, collect.toArray(new Robot[0]), null));
			} catch (Throwable t) {
				completedOrders.put(order.getOrderId(), new RealizedOrder(order, null, null, t));
				t.printStackTrace();
			}
			orderQueue.remove(order.getOrderId());
		}

		private Robot get(CompletableFuture<Robot> robot) {
			try {
				return robot.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			return null;
		}

		private Collection<CompletableFuture<Robot>> dispatch(RobotOrderLineItem[] lineItems) {
			Collection<CompletableFuture<Robot>> robots = new ArrayList<CompletableFuture<Robot>>();
			for (RobotOrderLineItem lineItem : lineItems) {
				final CompletableFuture<Robot> future = new CompletableFuture<Robot>();
				CompletableFuture.supplyAsync(dispatch(lineItem))
						.thenAccept((serial) -> schedulePollingForCompletion(serial, future));
				robots.add(future);
			}
			return robots;
		}

		private void schedulePollingForCompletion(Long serial, CompletableFuture<Robot> future) {
			RobotPickupJob job = new RobotPickupJob(serial, future);
			job.scheduledFuture = completionPollExecutor.scheduleAtFixedRate(job, 50, 9000, TimeUnit.MILLISECONDS);
		}

		private Supplier<Long> dispatch(RobotOrderLineItem lineItem) {
			return new RobotFactoryRequestSerialSupplier(lineItem);
		}

		private Customer validateUser(Long customerId) throws ValidationException {
			Request req = new Request.Builder().url(CUSTOMER_SERVICE_LOCATION + "/customers/" + customerId).get()
					//.tag(new TagWrapper(parentSpan.context()))
					.build();

			Response res = null;
			try {
				res = httpClient.newCall(req).execute();
				if (res.code() == Status.NOT_FOUND.getStatusCode()) {
					throw new ValidationException("Could not find customer " + customerId);
				}
				return Customer.fromJSon(res.body().string());
			} catch (IOException exc) {
				throw new ValidationException("Failed to validate customer");
			} finally {
			}
		}
	}

	public RobotOrder createNewOrder(long customerId, RobotOrderLineItem[] lineItems) {
		return new RobotOrder(SERIAL_ID_GENERATOR.getAndIncrement(), customerId, ZonedDateTime.now(), lineItems);
	}

	public void dispatchOrder(RobotOrder order) {
		orderQueue.put(order.getOrderId(), order);
		orderDispatcher.execute(new OrderJob(order));
	}

	/**
	 * @return the robots available for pick-up.
	 */
	public Collection<RealizedOrder> getCompletedOrders() {
		return completedOrders.values();
	}

	/**
	 * @param orderId
	 *            the order to pick up.
	 * @return null, if there was no such order ready for pick up yet (not completed), or the
	 *         completed order.
	 */
	public RealizedOrder pickUpOrder(Long orderId) {
		return completedOrders.remove(orderId);
	}

	public static OrderManager getInstance() {
		return INSTANCE;
	}
}
