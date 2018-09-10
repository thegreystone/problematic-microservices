package se.hirt.examples.robotshop.common.opentracing;

import io.opentracing.util.GlobalTracer;
import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class OpenTracingUtilTests extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public OpenTracingUtilTests(String testName) {
		super(testName);
	}

	public void testPropertyLoading() {
		OpenTracingUtil.configureOpenTracing("TestComponent");
		assertNotNull(GlobalTracer.get());
	}
}
