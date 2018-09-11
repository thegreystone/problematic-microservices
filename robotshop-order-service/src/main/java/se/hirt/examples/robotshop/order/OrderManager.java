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
package se.hirt.examples.robotshop.order;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response.Status;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.contrib.okhttp3.TracingCallFactory;
import io.opentracing.util.GlobalTracer;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se.hirt.examples.robotshop.common.data.Customer;
import se.hirt.examples.robotshop.common.data.RealizedOrder;
import se.hirt.examples.robotshop.common.data.Robot;
import se.hirt.examples.robotshop.common.data.RobotOrder;
import se.hirt.examples.robotshop.common.data.RobotOrderLineItem;
import se.hirt.examples.robotshop.common.data.RobotType;
import se.hirt.examples.robotshop.common.data.ValidationException;
import se.hirt.examples.robotshop.common.opentracing.SpanDecorator;

/**
 * Order manager, tracking and fulfilling robot orders.
 * 
 * @author Marcus Hirt
 */
public class OrderManager {
	private final static int JOB_QUEUE_SIZE = 500;
	private final static String CUSTOMER_SERVICE_LOCATION;
	private final static String FACTORY_SERVICE_LOCATION;

	private final static String DEFAULT_CUSTOMER_SERVICE_LOCATION = "http://localhost:8081";
	private final static String DEFAULT_FACTORY_SERVICE_LOCATION = "http://localhost:8082";

	private final static OrderManager INSTANCE = new OrderManager();
	private final static int DEFAULT_NUMBER_OF_ORDER_DISPATCHERS = 4;
	private final static AtomicLong SERIAL_ID_GENERATOR = new AtomicLong();

	private final Map<Long, RobotOrder> orderQueue = new ConcurrentHashMap<>();
	private final Map<Long, RealizedOrder> completedOrders = new ConcurrentHashMap<>();
	private final BlockingQueue<Runnable> jobQueue = new ArrayBlockingQueue<>(JOB_QUEUE_SIZE);
	private final Executor orderDispatcher = new ThreadPoolExecutor(0, DEFAULT_NUMBER_OF_ORDER_DISPATCHERS, 60,
			TimeUnit.SECONDS, jobQueue);
	private final ScheduledExecutorService completionPollExecutor = Executors.newScheduledThreadPool(4);

	private final Call.Factory httpClient = new TracingCallFactory(new OkHttpClient(), GlobalTracer.get(),
			SpanDecorator.getSpanDecorators());

	static {
		// Setting up service locations...
		String robotFactory = System.getenv("FACTORY_SERVICE_LOCATION");
		if (robotFactory == null) {
			robotFactory = DEFAULT_FACTORY_SERVICE_LOCATION;
		}
		String customerService = System.getenv("CUSTOMER_SERVICE_LOCATION");
		if (customerService == null) {
			customerService = DEFAULT_CUSTOMER_SERVICE_LOCATION;
		}
		FACTORY_SERVICE_LOCATION = robotFactory;
		CUSTOMER_SERVICE_LOCATION = customerService;
	}

	private final class RobotPickupJob implements Runnable {
		private final Long serial;
		private final CompletableFuture<Robot> future;
		private final SpanContext parent;

		public volatile ScheduledFuture<?> scheduledFuture;
		private volatile boolean isDone = false;

		public RobotPickupJob(Long serial, CompletableFuture<Robot> future, SpanContext parent) {
			this.serial = serial;
			this.future = future;
			this.parent = parent;
		}

		@Override
		public void run() {
			if (isDone) {
				scheduledFuture.cancel(false);
				return;
			}

			SpanBuilder buildSpan = GlobalTracer.get().buildSpan("pickupFromFactoryAttempt");
			buildSpan.withTag(Robot.KEY_SERIAL_NUMBER, String.valueOf(serial));
			buildSpan.asChildOf(parent);

			try (Scope scope = buildSpan.startActive(true)) {
				okhttp3.HttpUrl.Builder httpBuilder = HttpUrl.parse(FACTORY_SERVICE_LOCATION + "/factory/pickup")
						.newBuilder();
				httpBuilder.addQueryParameter(Robot.KEY_SERIAL_NUMBER, String.valueOf(serial));
				Request request = new Request.Builder().url(httpBuilder.build()).build();
				try {
					Response response = httpClient.newCall(request).execute();
					String body = response.body().string();
					if (response.isSuccessful() && !body.isEmpty()) {
						isDone = true;
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
	}

	private final class RobotFactoryRequestSerialSupplier implements Supplier<Long> {
		private final RobotOrderLineItem lineItem;
		private final SpanContext parent;

		public RobotFactoryRequestSerialSupplier(RobotOrderLineItem lineItem, SpanContext parent) {
			this.lineItem = lineItem;
			this.parent = parent;
		}

		@Override
		public Long get() {
			SpanBuilder buildSpan = GlobalTracer.get().buildSpan("buildRobotRequest");
			buildSpan.withTag(RobotType.KEY_ROBOT_TYPE, lineItem.getRobotTypeId());
			buildSpan.withTag(Robot.KEY_COLOR, lineItem.getColor().toString());
			buildSpan.asChildOf(parent);

			try (Scope scope = buildSpan.startActive(true)) {
				FormBody.Builder formBuilder = new FormBody.Builder();
				formBuilder.add(RobotType.KEY_ROBOT_TYPE, lineItem.getRobotTypeId());
				formBuilder.add(Robot.KEY_COLOR, lineItem.getColor().toString());

				Request req = new Request.Builder().url(FACTORY_SERVICE_LOCATION + "/factory/buildrobot")
						.post(formBuilder.build()).build();

				try {
					Response response = httpClient.newCall(req).execute();
					return parseSerial(response.body().string());
				} catch (Throwable ioe) {
					ioe.printStackTrace();
				}
			}
			return -1L;
		}

		private Long parseSerial(String json) {
			JsonObject readObject = Json.createReader(new StringReader(json)).readObject();
			String serialString = readObject.getString(Robot.KEY_SERIAL_NUMBER);
			if (serialString == null) {
				return Robot.INVALID_SERIAL_ID;
			}
			return Long.valueOf(serialString);
		}
	}

	private final class OrderJob implements Runnable {
		private final RobotOrder order;
		private final SpanContext initiatior;

		public OrderJob(RobotOrder order, SpanContext initiator) {
			this.order = order;
			this.initiatior = initiator;
		}

		@Override
		public void run() {
			try {
				SpanBuilder buildSpan = GlobalTracer.get().buildSpan("orderJob");
				buildSpan.withTag(RobotOrder.KEY_ORDER_ID, String.valueOf(order.getOrderId()));
				buildSpan.addReference(References.FOLLOWS_FROM, initiatior);
				try (Scope scope = buildSpan.startActive(true)) {
					Customer customer = validateUser(order.getCustomerId(), scope.span().context());
					Collection<CompletableFuture<Robot>> robots = dispatch(order.getLineItems(),
							scope.span().context());
					CompletableFuture<Void> allOf = CompletableFuture.allOf(robots.toArray(new CompletableFuture[0]));
					allOf.get();
					List<Robot> collect = robots.stream().map((robot) -> get(robot)).collect(Collectors.toList());

					// TODO verify that all list items got realized - otherwise add errors for the ones missing etc
					completedOrders.put(order.getOrderId(),
							new RealizedOrder(order, customer, collect.toArray(new Robot[0]), null));
				}
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

		private Collection<CompletableFuture<Robot>> dispatch(RobotOrderLineItem[] lineItems, SpanContext spanContext) {
			Collection<CompletableFuture<Robot>> robots = new ArrayList<CompletableFuture<Robot>>();
			for (RobotOrderLineItem lineItem : lineItems) {
				final CompletableFuture<Robot> future = new CompletableFuture<Robot>();
				CompletableFuture.supplyAsync(dispatch(lineItem, spanContext))
						.thenAccept((serial) -> schedulePollingForCompletion(serial, future, spanContext));
				robots.add(future);
			}
			return robots;
		}

		private void schedulePollingForCompletion(Long serial, CompletableFuture<Robot> future, SpanContext parent) {
			RobotPickupJob job = new RobotPickupJob(serial, future, parent);
			job.scheduledFuture = completionPollExecutor.scheduleAtFixedRate(job, 50, 9000, TimeUnit.MILLISECONDS);
		}

		private Supplier<Long> dispatch(RobotOrderLineItem lineItem, SpanContext parent) {
			return new RobotFactoryRequestSerialSupplier(lineItem, parent);
		}

		private Customer validateUser(Long customerId, SpanContext parent) throws ValidationException {
			SpanBuilder spanBuilder = GlobalTracer.get().buildSpan("validateUser")
					.withTag(Customer.KEY_CUSTOMER_ID, String.valueOf(customerId)).asChildOf(parent);
			try (Scope scope = spanBuilder.startActive(true)) {
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
				}
			}
		}
	}

	public RobotOrder createNewOrder(long customerId, RobotOrderLineItem[] lineItems) {
		return new RobotOrder(SERIAL_ID_GENERATOR.getAndIncrement(), customerId, ZonedDateTime.now(), lineItems);
	}

	/**
	 * @param order
	 * @param spanContext
	 *            need to pass the open tracing context for async processing. FIXME: Got to be a
	 *            better way than passing around non-functional stuff.
	 */
	public void dispatchOrder(RobotOrder order, SpanContext spanContext) {
		orderQueue.put(order.getOrderId(), order);
		orderDispatcher.execute(new OrderJob(order, spanContext));
	}

	/**
	 * @return the current orders in-flight.
	 */
	public Collection<RobotOrder> getActiveOrders() {
		return orderQueue.values();
	}

	public RobotOrder getActiveOrderById(Long id) {
		return orderQueue.get(id);
	}

	/**
	 * @return the robots available for pick-up.
	 */
	public Collection<RealizedOrder> getCompletedOrders() {
		return completedOrders.values();
	}

	public RealizedOrder getRealizedOrderById(Long id) {
		return completedOrders.get(id);
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
