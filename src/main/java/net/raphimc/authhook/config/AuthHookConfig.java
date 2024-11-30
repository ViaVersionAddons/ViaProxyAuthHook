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
package net.raphimc.authhook.config;

import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.annotations.*;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.net.SocketAddress;
import java.util.UUID;

@OptConfig
public class AuthHookConfig {

    @Option("secret-key")
    @Description("The secret key used to verify the servers. Paste this key into the auth_hook.properties config file on your server.")
    public static String secretKey = UUID.randomUUID().toString().replace("-", "");

    @NotReloadable
    @Option("bind-address")
    @Description({"The address AuthHook should listen for HTTP requests."})
    @TypeSerializer(SocketAddressTypeSerializer.class)
    public static SocketAddress bindAddress = AddressUtil.parse("127.0.0.1:8080", null);

    public static void load(final File dataFolder) {
        try {
            final ConfigLoader<AuthHookConfig> configLoader = new ConfigLoader<>(AuthHookConfig.class);
            configLoader.getConfigOptions().setResetInvalidOptions(true);
            configLoader.loadStatic(ConfigProvider.file(new File(dataFolder, "auth_hook.yml")));
        } catch (Throwable t) {
            Logger.LOGGER.error("Failed to load the AuthHook configuration!", t);
            System.exit(-1);
        }
    }

}
