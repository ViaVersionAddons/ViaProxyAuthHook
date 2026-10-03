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

import com.google.common.cache.CacheBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.raphimc.netminecraft.util.EventLoops;
import net.raphimc.netminecraft.util.TransportType;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {

    private final ChannelFuture channelFuture;
    private final Map<String, ProxyConnection> pendingConnections = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).<String, ProxyConnection>build().asMap();
    private final HttpClient httpClient = HttpClient.newBuilder().executor(Executors.newCachedThreadPool()).build();

    public HttpServer(final InetSocketAddress bindAddress) {
        this.channelFuture = new ServerBootstrap()
            .group(EventLoops.getClientEventLoop(TransportType.NIO))
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new HttpServerChannelInitializer(this.pendingConnections, this.httpClient))
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
            } catch (final Exception ignored) {
            }
        }
    }

    public Channel getChannel() {
        return this.channelFuture.channel();
    }

}
