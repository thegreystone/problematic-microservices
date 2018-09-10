package se.hirt.examples.robotshop.common.opentracing;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.HttpServletRequestExtractAdapter;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

public class OpenTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	public static final String SERVER_SPAN_CONTEXT = OpenTracingFilter.class.getName() + ".activeSpanContext";
	public static final String SERVER_SPAN_SCOPE = OpenTracingFilter.class.getName() + ".activeSpanScope";

	private final Tracer tracer;

	@Context
	private HttpServletRequest httpRequest;

	public OpenTracingFilter() {
		this (GlobalTracer.get());
	}

	public OpenTracingFilter(Tracer tracer) {
		this.tracer = tracer;
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		if (isTraced(requestContext)) {
			Scope scope = (Scope) httpRequest.getAttribute(SERVER_SPAN_SCOPE);
			scope.span().finish();
			scope.close();
		}
	}

	private boolean isTraced(ContainerRequestContext requestContext) {
		return requestContext.getProperty(SERVER_SPAN_SCOPE) != null;
	}

	/**
	 * Get context of server span.
	 *
	 * @param servletRequest
	 *            request
	 * @return server span context
	 */
	public SpanContext serverSpanContext(ServletRequest servletRequest) {
		return (SpanContext) httpRequest.getAttribute(SERVER_SPAN_CONTEXT);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (!isTraced(requestContext)) {
			SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
					new HttpServletRequestExtractAdapter(httpRequest));

			final Scope scope = tracer.buildSpan(httpRequest.getMethod()).asChildOf(extractedContext)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(false);

			httpRequest.setAttribute(SERVER_SPAN_SCOPE, scope);
		} else {

		}
	}

}
