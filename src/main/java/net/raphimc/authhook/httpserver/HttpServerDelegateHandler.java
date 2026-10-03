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

import com.viaversion.viaversion.util.Either;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.raphimc.authhook.config.AuthHookConfig;
import net.raphimc.authhook.httpserver.handler.DiscoveryHandler;
import net.raphimc.authhook.httpserver.handler.RedirectedHostHandler;
import net.raphimc.authhook.httpserver.handler.SessionServiceHandler;
import net.raphimc.authhook.httpserver.model.ForwardedRequest;
import net.raphimc.authhook.httpserver.model.ForwardedResponse;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class HttpServerDelegateHandler extends MessageToMessageCodec<HttpRequest, ForwardedResponse> {

    private final Map<String, RedirectedHostHandler> handlers;

    public HttpServerDelegateHandler(final Map<String, ProxyConnection> pendingConnections) {
        this.handlers = Map.of(
            "https://sessionserver.mojang.com", new SessionServiceHandler(pendingConnections),
            "https://discovery.minecraftservices.com", new DiscoveryHandler()
        );
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final HttpRequest request, final List<Object> list) {
        if (!request.uri().startsWith("/" + AuthHookConfig.secretKey + "/")) {
            ctx.close();
            return;
        }

        final String requestHost = request.headers().get(HttpHeaders.HOST);
        String uri = request.uri().substring(AuthHookConfig.secretKey.length() + 2);
        final String originalHost = new String(Base64.getUrlDecoder().decode(uri.substring(0, uri.indexOf('/'))), StandardCharsets.UTF_8);
        uri = uri.substring(uri.indexOf('/'));
        request.setUri(uri);

        final RedirectedHostHandler handler = this.handlers.get(originalHost);
        if (handler == null) {
            ctx.close();
            return;
        }

        final Either<URI, FullHttpResponse> either = handler.handleRequest(originalHost, request);
        if (either.isLeft()) {
            list.add(new ForwardedRequest(handler, requestHost, request, either.left()));
        } else {
            ctx.writeAndFlush(either.right())
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    protected void encode(final ChannelHandlerContext channelHandlerContext, final ForwardedResponse forwardedResponse, final List<Object> list) {
        final FullHttpResponse response = forwardedResponse.handler().handleResponse(forwardedResponse);
        if (response != null) {
            list.add(response);
        }
    }

}
