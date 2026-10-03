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
package net.raphimc.authhook.httpserver.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.util.Either;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.URI;
import java.util.Map;

public class SessionServiceHandler implements RedirectedHostHandler {

    private final Map<String, ProxyConnection> pendingConnections;

    public SessionServiceHandler(final Map<String, ProxyConnection> pendingConnections) {
        this.pendingConnections = pendingConnections;
    }

    @Override
    public Either<URI, FullHttpResponse> handleRequest(final String host, final HttpRequest request) {
        if (request.method().equals(HttpMethod.GET) && request.uri().startsWith("/session/minecraft/hasJoined")) {
            final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
            if (queryStringDecoder.parameters().containsKey("username") && queryStringDecoder.parameters().containsKey("serverId")) {
                final String username = queryStringDecoder.parameters().get("username").get(0);
                final String serverId = queryStringDecoder.parameters().get("serverId").get(0);

                final ProxyConnection proxyConnection = this.pendingConnections.remove(serverId + "_" + username);
                if (proxyConnection != null) {
                    final GameProfile gameProfile = proxyConnection.getGameProfile();
                    final JsonObject responseObj = new JsonObject();
                    responseObj.addProperty("name", gameProfile.getName());
                    responseObj.addProperty("id", gameProfile.getId().toString().replace("-", ""));
                    if (!gameProfile.getProperties().isEmpty()) {
                        final JsonArray propertiesArray = new JsonArray();
                        gameProfile.getProperties().forEach((key, value) -> {
                            final JsonObject propertyObj = new JsonObject();
                            propertyObj.addProperty("name", key);
                            propertyObj.addProperty("value", value.value());
                            if (value.hasSignature()) {
                                propertyObj.addProperty("signature", value.signature());
                            }
                            propertiesArray.add(propertyObj);
                        });
                        responseObj.add("properties", propertiesArray);
                    }

                    final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer());
                    response.content().writeBytes(responseObj.toString().getBytes());
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                    return Either.right(response);
                }
            }
        }
        return Either.left(URI.create(host + request.uri()));
    }

}
