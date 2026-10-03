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
package net.raphimc.authhook.httpserver.model;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.raphimc.authhook.httpserver.handler.RedirectedHostHandler;

import java.net.URI;

public record ForwardedResponse(RedirectedHostHandler handler, String requestHost, URI uri, FullHttpResponse response) {

    public ForwardedResponse(final ForwardedRequest request, final FullHttpResponse response) {
        this(request.handler(), request.requestHost(), request.uri(), response);
    }

    public byte[] getContent() {
        return ByteBufUtil.getBytes(this.response.content());
    }

    public void setContent(final byte[] content) {
        this.response.content().clear();
        this.response.content().writeBytes(content);
        this.response.headers().set(HttpHeaders.CONTENT_LENGTH, content.length);
    }

}
