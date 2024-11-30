/*
 * This file is part of ViaProxyAuthHook - https://github.com/ViaVersionAddons/ViaProxyAuthHook
 * Copyright (C) 2024-2024 RK_01/RaphiMC and contributors
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

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.reflect.Enums;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.authhook.config.AuthHookConfig;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.plugins.events.JoinServerRequestEvent;
import net.raphimc.viaproxy.plugins.events.ViaProxyLoadedEvent;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;

public class AuthHook extends ViaProxyPlugin {

    private static ViaProxyConfig.AuthMethod AUTH_HOOK;
    private AuthHookHttpServer authHookHttpServer;

    @Override
    public void onEnable() {
        ViaProxy.EVENT_MANAGER.register(this);
        AuthHookConfig.load(this.getDataFolder());

        this.authHookHttpServer = new AuthHookHttpServer((InetSocketAddress) AuthHookConfig.bindAddress);
        Logger.LOGGER.info("AuthHook is listening on http://" + AuthHookConfig.bindAddress);

        AUTH_HOOK = Enums.newInstance(ViaProxyConfig.AuthMethod.class, "AUTH_HOOK", ViaProxyConfig.AuthMethod.values().length, new Class[]{String.class}, new Object[]{"authhook.auth_method.name"});
        Enums.addEnumInstance(ViaProxyConfig.AuthMethod.class, AUTH_HOOK);
    }

    @EventHandler
    private void onViaProxyLoaded(ViaProxyLoadedEvent event) {
        if (!ViaProxy.getConfig().isProxyOnlineMode()) {
            Logger.LOGGER.error("Proxy online mode is disabled, please enable it to use the AuthHook plugin!");
            Logger.LOGGER.error("Without online mode the AuthHook plugin would be effectively useless");
            Logger.LOGGER.error("Shutting down...");
            System.exit(0);
        }

        final Map<String, Properties> locales = RStream.of(I18n.class).fields().by("LOCALES").get();
        locales.get("en_US").setProperty(AUTH_HOOK.getGuiTranslationKey(), "Use AuthHook");
    }

    @EventHandler
    private void onJoinServerRequest(JoinServerRequestEvent event) {
        if (ViaProxy.getConfig().getAuthMethod() == AUTH_HOOK) {
            this.authHookHttpServer.addPendingConnection(event.getServerIdHash(), event.getProxyConnection());
            event.setCancelled(true);
        }
    }

}
