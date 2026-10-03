/*
 * This file is part of ViaProxyAuthHook - https://github.com/ViaVersionAddons/ViaProxyAuthHook
 * Copyright (C) 2024-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.authhook.httpserver;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import net.raphimc.authhook.httpserver.model.ForwardedRequest;
import net.raphimc.authhook.httpserver.model.ForwardedResponse;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class HttpServerForwardHandler extends SimpleChannelInboundHandler<ForwardedRequest> {

    private final HttpClient httpClient;

    public HttpServerForwardHandler(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ForwardedRequest request) {
        final HttpRequest.Builder httpRequest = HttpRequest.newBuilder().uri(request.uri());

        for (Map.Entry<String, String> header : request.request().headers()) {
            try {
                httpRequest.setHeader(header.getKey(), header.getValue());
            } catch (final Throwable t) {
                // Ignore invalid headers
                // Some headers are restricted so you can't set them (e.g. "Host")
            }
        }

        final HttpRequest.BodyPublisher bodyPublisher;
        if (request.request() instanceof FullHttpRequest fullHttpRequest) {
            final byte[] body = new byte[fullHttpRequest.content().readableBytes()];
            fullHttpRequest.content().readBytes(body);
            if (body.length > 0) {
                bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(body);
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            }
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }
        httpRequest.method(request.request().method().name(), bodyPublisher);
        // "Content-Length" is also restricted and automatically set by Java

        this.httpClient.sendAsync(httpRequest.build(), HttpResponse.BodyHandlers.ofByteArray()).thenAccept(response -> {
            final FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode()), ctx.alloc().buffer());
            fullHttpResponse.content().writeBytes(response.body());
            for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
                if (!entry.getKey().startsWith(":")) {
                    fullHttpResponse.headers().set(entry.getKey(), entry.getValue().get(0));
                }
            }
            fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ctx.writeAndFlush(new ForwardedResponse(request, fullHttpResponse))
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener.CLOSE);
        });
    }

}
