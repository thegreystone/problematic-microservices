package se.hirt.examples.robotshop.common.opentracing;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class OpenTracingDynamicFeature implements DynamicFeature {

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		final String resourcePackage = resourceInfo.getResourceClass().getPackage().getName();
		// Check annotations? resourceMethod.getAnnotation(GET.class) != null)...
		// final Method resourceMethod = resourceInfo.getResourceMethod();

		if (resourcePackage != null && resourcePackage.startsWith("se.hirt.examples.robotshop")) {
			context.register(OpenTracingFilter.class);
		}
	}

}
