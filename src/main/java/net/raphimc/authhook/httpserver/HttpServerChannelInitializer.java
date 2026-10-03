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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpServerCodec;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.http.HttpClient;
import java.util.Map;

public class HttpServerChannelInitializer extends ChannelInitializer<Channel> {

    private final Map<String, ProxyConnection> pendingConnections;
    private final HttpClient httpClient;

    public HttpServerChannelInitializer(final Map<String, ProxyConnection> pendingConnections, final HttpClient httpClient) {
        this.pendingConnections = pendingConnections;
        this.httpClient = httpClient;
    }

    @Override
    protected void initChannel(final Channel channel) {
        channel.pipeline().addLast("http_codec", new HttpServerCodec());
        channel.pipeline().addLast("delegate_handler", new HttpServerDelegateHandler(this.pendingConnections));
        channel.pipeline().addLast("forward_handler", new HttpServerForwardHandler(this.httpClient));
    }

}
