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

import io.netty.handler.codec.http.FullHttpResponse;
import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.authhook.config.AuthHookConfig;
import net.raphimc.authhook.httpserver.model.ForwardedResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DiscoveryHandler implements RedirectedHostHandler {

    @Override
    public FullHttpResponse handleResponse(final ForwardedResponse forwardedResponse) {
        if (forwardedResponse.uri().getPath().equals("/minecraft/client")) {
            final String response = new String(forwardedResponse.getContent(), StandardCharsets.UTF_8);
            final GsonObject root = GsonParser.parse(response).asObject();
            final GsonObject verify = root.optObject("discovery")
                .flatMap(discovery -> discovery.optObject("session"))
                .flatMap(session -> session.optObject("endpoints"))
                .flatMap(endpoints -> endpoints.optObject("verify"))
                .orElse(null);
            if (verify != null && verify.hasString("uri")) {
                final String uri = verify.getString("uri");
                verify.add("uri", this.modifyUri(uri, forwardedResponse.requestHost()));

                forwardedResponse.setContent(root.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return forwardedResponse.response();
    }

    private String modifyUri(final String uri, final String requestHost) {
        final URLWrapper wrapper = URLWrapper.ofURI(uri);
        wrapper.setPath("/" + AuthHookConfig.secretKey
            + "/" + Base64.getUrlEncoder().encodeToString((wrapper.getProtocol() + "://" + wrapper.getHost()).getBytes(StandardCharsets.UTF_8))
            + wrapper.getPath()
        );
        wrapper.setProtocol("http");
        wrapper.setHost(requestHost);
        return wrapper.toString();
    }

}
