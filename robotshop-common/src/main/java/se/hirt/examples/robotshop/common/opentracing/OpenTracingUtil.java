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
package se.hirt.examples.robotshop.common.opentracing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.util.GlobalTracer;
import se.hirt.examples.robotshop.common.util.Logger;
import se.hirt.examples.robotshop.common.util.Utils;
import se.hirt.jmc.opentracing.DelegatingJfrTracer;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Toolkit to handle some open tracing configuration.
 * 
 * @author Marcus Hirt
 */
public class OpenTracingUtil {
	private final static String DEFAULT_CONFIGURATION_RESOURCE_NAME = "tracing.properties";
	private final static String CONFIGURATION_LOCATION = System.getProperty("tracingConfiguraton");
	private final static Properties CONFIGURATION;

	static {
		// Setting up logging just before initing open tracing.
		Utils.initLogging();
		CONFIGURATION = loadProperties();
	}

	private static Properties loadProperties() {
		Properties configuration = new Properties();
		if (CONFIGURATION_LOCATION == null) {
			try (InputStream is = OpenTracingUtil.class.getClassLoader()
					.getResourceAsStream(DEFAULT_CONFIGURATION_RESOURCE_NAME)) {
				configuration.load(is);
			} catch (Exception ie) {
				Logger.log("Could not find the default Open Tracing configuration.");
				// This should never happen...
				System.out.flush();
				System.exit(4711);
			}
		} else {
			try (InputStream is = new FileInputStream(new File(CONFIGURATION_LOCATION))) {
				configuration.load(is);
			} catch (Exception ie) {
				Logger.log("Could not find the Open Tracing configuration at " + CONFIGURATION_LOCATION + ".");
				System.out.flush();
				System.exit(4711);
			}
		}
		return configuration;
	}

	/**
	 * Configures open tracing from the properties read, either from the default file, or as
	 * configured by the tracingConfiguration properties file.
	 * 
	 * @param componentName
	 *            the name of the component.
	 */
	public static void configureOpenTracing(String componentName) {
		configureOpenTracing(CONFIGURATION, componentName);
	}

	/**
	 * Returns a map with the information appropriate to log on exception.
	 * 
	 * @param t
	 *            the exception to log.
	 * @return the map with information suitable for the {@link io.opentracing.Span#log(Map)}
	 *         method.
	 */
	public static Map<String, Object> getSpanLogMap(Throwable t) {
		// Want to keep this compilable with JDK 8, so not using Map.of...
		Map<String, Object> map = new HashMap<>();
		map.put(Fields.EVENT, "error");
		map.put(Fields.ERROR_OBJECT, t);
		map.put(Fields.MESSAGE, t.getMessage());
		return map;
	}

	private static void configureOpenTracing(Properties configuration, String serviceName) {
		Tracer tracer = null;
		String tracerName = configuration.getProperty("tracer");
		if ("jaeger".equals(tracerName)) {
			SamplerConfiguration samplerConfig = new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1);
			SenderConfiguration senderConfig = new SenderConfiguration()
					.withAgentHost(configuration.getProperty("jaeger.reporter.host"))
					.withAgentPort(Integer.decode(configuration.getProperty("jaeger.reporter.port")));
			ReporterConfiguration reporterConfig = new ReporterConfiguration().withLogSpans(true)
					.withFlushInterval(1000).withMaxQueueSize(10000).withSender(senderConfig);
			tracer = new Configuration(serviceName).withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
		} else if ("zipkin".equals(tracerName)) {
			OkHttpSender sender = OkHttpSender.create("http://" + configuration.getProperty("zipkin.reporter.host")
					+ ":" + configuration.getProperty("zipkin.reporter.port") + "/api/v2/spans");
			Reporter<Span> reporter = AsyncReporter.builder(sender).build();
			tracer = BraveTracer
					.create(Tracing.newBuilder().localServiceName(serviceName).spanReporter(reporter).build());
		}
		GlobalTracer.register(new DelegatingJfrTracer(tracer));
	}
}
