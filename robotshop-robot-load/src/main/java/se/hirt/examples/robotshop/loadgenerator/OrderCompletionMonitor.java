package se.hirt.examples.robotshop.loadgenerator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.contrib.okhttp3.TracingCallFactory;
import io.opentracing.util.GlobalTracer;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se.hirt.examples.robotshop.common.data.RealizedOrder;
import se.hirt.examples.robotshop.common.data.RobotOrder;
import se.hirt.examples.robotshop.common.opentracing.SpanDecorator;

public class OrderCompletionMonitor implements Runnable {
	private final Call.Factory httpClient = new TracingCallFactory(new OkHttpClient(), GlobalTracer.get(),
			SpanDecorator.getSpanDecorators());

	private final String pickupLocation;
	private final RobotOrder order;
	private final SpanContext parent;
	private final CompletableFuture<RealizedOrder> future;
	public volatile ScheduledFuture<?> scheduledFuture;
	private volatile boolean isDone;

	public OrderCompletionMonitor(String pickupLocation, RobotOrder order, CompletableFuture<RealizedOrder> future,
			SpanContext parent) {
		this.pickupLocation = pickupLocation;
		this.order = order;
		this.future = future;
		this.parent = parent;
	}

	@Override
	public void run() {
		if (isDone) {
			scheduledFuture.cancel(false);
			return;
		}

		SpanBuilder buildSpan = GlobalTracer.get().buildSpan("pickupOrderAttempt");
		buildSpan.withTag(RobotOrder.KEY_ORDER_ID, String.valueOf(order.getOrderId()));
		buildSpan.addReference(References.FOLLOWS_FROM, parent);

		try (Scope scope = buildSpan.startActive(true)) {
			okhttp3.HttpUrl.Builder httpBuilder = HttpUrl.parse(pickupLocation).newBuilder();
			httpBuilder.addQueryParameter(RobotOrder.KEY_ORDER_ID, String.valueOf(order.getOrderId()));
			Request request = new Request.Builder().url(httpBuilder.build()).build();
			try {
				Response response = httpClient.newCall(request).execute();
				String body = response.body().string();
				if (response.isSuccessful() && !body.isEmpty()) {
					isDone = true;
					RealizedOrder realizedOrder = RealizedOrder.fromJSon(body);
					future.complete(realizedOrder);
					scheduledFuture.cancel(false);
				}
			} catch (IOException e) {
				e.printStackTrace();
				scheduledFuture.cancel(false);
			}
		}
	}

}
