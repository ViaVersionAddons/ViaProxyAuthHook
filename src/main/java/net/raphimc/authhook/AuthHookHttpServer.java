/*
 * This file is part of ViaProxyAuthHook - https://github.com/ViaVersionAddons/ViaProxyAuthHook
 * Copyright (C) 2024-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.authhook;

import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import net.raphimc.authhook.config.AuthHookConfig;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuthHookHttpServer {

    private final InetSocketAddress bindAddress;
    private final ChannelFuture channelFuture;
    private final Map<String, ProxyConnection> pendingConnections = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).<String, ProxyConnection>build().asMap();
    private final HttpClient httpClient = HttpClient.newBuilder().executor(Executors.newCachedThreadPool()).build();

    public AuthHookHttpServer(final InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
        this.channelFuture = new ServerBootstrap()
                .group(new NioEventLoopGroup(0))
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast("http_codec", new HttpServerCodec());
                        channel.pipeline().addLast("http_handler", new SimpleChannelInboundHandler<>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                                if (!(msg instanceof HttpRequest request)) {
                                    return;
                                }

                                if (request.uri().startsWith("/" + AuthHookConfig.secretKey + "/")) {
                                    final String uri = request.uri().substring(AuthHookConfig.secretKey.length() + 1);
                                    if (request.method().equals(HttpMethod.GET) && uri.startsWith("/session/minecraft/hasJoined")) {
                                        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
                                        if (queryStringDecoder.parameters().containsKey("username") && queryStringDecoder.parameters().containsKey("serverId")) {
                                            final String username = queryStringDecoder.parameters().get("username").get(0);
                                            final String serverId = queryStringDecoder.parameters().get("serverId").get(0);

                                            final ProxyConnection proxyConnection = pendingConnections.remove(serverId + "_" + username);
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
                                                        propertyObj.addProperty("value", value.getValue());
                                                        if (value.hasSignature()) {
                                                            propertyObj.addProperty("signature", value.getSignature());
                                                        }
                                                        propertiesArray.add(propertyObj);
                                                    });
                                                    responseObj.add("properties", propertiesArray);
                                                }

                                                final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer());
                                                response.content().writeBytes(responseObj.toString().getBytes());
                                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                                                ctx.writeAndFlush(response).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE);
                                                return;
                                            }
                                        }
                                    }

                                    httpClient.sendAsync(java.net.http.HttpRequest.newBuilder().uri(URI.create("https://sessionserver.mojang.com" + uri)).build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray())
                                            .thenAccept(response -> {
                                                final FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode()), ctx.alloc().buffer());
                                                fullHttpResponse.content().writeBytes(response.body());
                                                for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
                                                    if (!entry.getKey().startsWith(":")) {
                                                        fullHttpResponse.headers().set(entry.getKey(), entry.getValue().get(0));
                                                    }
                                                }
                                                fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                                                ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE);
                                            });
                                } else {
                                    ctx.close();
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                ctx.close();
                            }
                        });
                    }
                })
                .bind(bindAddress)
                .syncUninterruptibly();
    }

    public void addPendingConnection(final String serverIdHash, final ProxyConnection connection) {
        this.pendingConnections.put(serverIdHash + "_" + connection.getGameProfile().getName(), connection);
    }

    public void stop() {
        if (this.channelFuture != null) {
            this.channelFuture.channel().close();
        }
        this.httpClient.executor().map(ExecutorService.class::cast).ifPresent(ExecutorService::shutdown);
        if (this.httpClient instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    public Channel getChannel() {
        return this.channelFuture.channel();
    }

}
