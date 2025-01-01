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
package net.lenni0451.authhook;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {

    private static final File PATH = new File("auth_hook.properties");
    public static final String TARGET_ADDRESS = "target_address";
    public static final String SECRET_KEY = "secret_key";

    private static Map<String, String> getDefaults() {
        Map<String, String> def = new LinkedHashMap<>();
        def.put(TARGET_ADDRESS, "http://localhost:8080");
        def.put(SECRET_KEY, "paste secret key from ViaProxy here");
        return def;
    }

    public static Map<String, String> load() throws IOException {
        Map<String, String> config = getDefaults();
        if (PATH.exists()) {
            try (Scanner scanner = new Scanner(PATH)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("#")) continue;
                    if (line.contains("=")) {
                        String[] split = line.split("=", 2);
                        config.put(split[0], split[1]);
                    }
                }
            }
        } else {
            save(config);
        }
        return config;
    }

    public static void save(final Map<String, String> config) throws IOException {
        try (FileWriter writer = new FileWriter(PATH)) {
            writer.write("# AuthHook Configuration File\n\n");
            for (Map.Entry<String, String> entry : config.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        }
    }

}
