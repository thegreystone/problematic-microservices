package se.hirt.examples.robotshop.common.opentracing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import se.hirt.examples.robotshop.common.util.Logger;
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

	private static void configureOpenTracing(Properties configuration, String componentName) {
		Tracer tracer = null;
		String tracerName = configuration.getProperty("tracer");
		if ("jaeger".equals(tracerName)) {
			SamplerConfiguration samplerConfig = new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1);
			SenderConfiguration senderConfig = new SenderConfiguration()
					.withAgentHost(configuration.getProperty("jaeger.reporter.host"))
					.withAgentPort(Integer.decode(configuration.getProperty("jaeger.reporter.port")));
			ReporterConfiguration reporterConfig = new ReporterConfiguration().withLogSpans(true)
					.withFlushInterval(1000).withMaxQueueSize(10000).withSender(senderConfig);
			tracer = new Configuration(componentName).withSampler(samplerConfig).withReporter(reporterConfig)
					.getTracer();
		} else if ("zipkin".equals(tracerName)) {
			OkHttpSender sender = OkHttpSender.create("http://" + configuration.getProperty("zipkin.reporter.host")
					+ ":" + configuration.getProperty("zipkin.reporter.port") + "/api/v1/spans");
			Reporter<Span> reporter = AsyncReporter.builder(sender).build();
			tracer = BraveTracer
					.create(Tracing.newBuilder().localServiceName(componentName).spanReporter(reporter).build());
		}
		// FIXME: Add delegating JFR tracer
		GlobalTracer.register(tracer);
	}
}
