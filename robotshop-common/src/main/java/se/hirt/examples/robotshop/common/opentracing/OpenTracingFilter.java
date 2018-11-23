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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.HttpServletRequestExtractAdapter;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * Filter to automatically create spans and for decoding them from requests. We will propagate them
 * through the okhttp client, so no need to do that here.
 * 
 * @author Marcus Hirt
 */
public class OpenTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	public static final String PROPERTY_KEEP_OPEN = "OT_KEEP_OPEN";
	public static final String SERVER_SPAN_CONTEXT = OpenTracingFilter.class.getName() + ".activeSpanContext";
	public static final String SERVER_SPAN_SCOPE = OpenTracingFilter.class.getName() + ".activeSpanScope";

	private final Tracer tracer;

	@Context
	private HttpServletRequest httpRequest;

	public OpenTracingFilter() {
		this(GlobalTracer.get());
	}

	public OpenTracingFilter(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		if (isTraced(requestContext) && !keepOpen(requestContext.getProperty(PROPERTY_KEEP_OPEN))) {
			Scope scope = getActiveScope(httpRequest);
			Span span = scope.span(); 
			scope.close();
			span.finish();
		}
	}

	private boolean keepOpen(Object property) {
		if (property == null) {
			return false;
		}
		if (property instanceof Boolean) {
			return (Boolean) property;
		}
		return true;
	}

	private boolean isTraced(ContainerRequestContext requestContext) {
		return requestContext.getProperty(SERVER_SPAN_SCOPE) != null;
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (!isTraced(requestContext)) {
			SpanContext extractedContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
					new HttpServletRequestExtractAdapter(httpRequest));

			final Scope scope = tracer.buildSpan(httpRequest.getRequestURL().toString()).asChildOf(extractedContext)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(false);

			httpRequest.setAttribute(SERVER_SPAN_SCOPE, scope);
			httpRequest.setAttribute(SERVER_SPAN_CONTEXT, scope.span().context());
		}
	}

	/**
	 * Returns the active scope.
	 * 
	 * @param request
	 *            the request from which to retrieve the scope.
	 * @return the active scope for the request.
	 */
	public static Scope getActiveScope(HttpServletRequest request) {
		return (Scope) request.getAttribute(SERVER_SPAN_SCOPE);
	}

	/**
	 * Returns the active scope.
	 * 
	 * @param request
	 *            the request from which to retrieve the scope.
	 * @return the active scope for the request.
	 */
	public static SpanContext getActiveContext(HttpServletRequest request) {
		return (SpanContext) request.getAttribute(SERVER_SPAN_CONTEXT);
	}

	/**
	 * Does not automatically close this request.
	 * 
	 * @param request
	 *            the request from which to retrieve the scope.
	 * @return the active scope for the request.
	 */
	public static void setKeepOpen(HttpServletRequest request, Boolean keepOpen) {
		request.setAttribute(PROPERTY_KEEP_OPEN, keepOpen);
	}
}
