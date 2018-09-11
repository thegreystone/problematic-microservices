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

import java.util.ArrayList;
import java.util.List;

import io.opentracing.Span;
import io.opentracing.contrib.okhttp3.OkHttpClientSpanDecorator;
import okhttp3.Connection;
import okhttp3.Request;
import okhttp3.Response;
/**
 * Span decorator for OkHttp client-use.
 * 
 * @author Marcus Hirt
 */
public final class SpanDecorator implements OkHttpClientSpanDecorator {

	@Override
	public void onRequest(Request request, Span span) {
		span.setOperationName("Call: " + request.url().toString());
	}

	@Override
	public void onError(Throwable throwable, Span span) {
		span.setOperationName("Error: " + throwable.getMessage());
	}

	@Override
	public void onResponse(Connection connection, Response response, Span span) {
		span.setOperationName("Response: " + response.code());
	}

	
	public static List<OkHttpClientSpanDecorator> getSpanDecorators() {
		List<OkHttpClientSpanDecorator> decorators = new ArrayList<>(1);
		decorators.add(new SpanDecorator());
		return decorators;
	}
}
