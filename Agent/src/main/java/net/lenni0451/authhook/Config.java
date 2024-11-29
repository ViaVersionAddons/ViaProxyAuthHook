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
        def.put(TARGET_ADDRESS, "http://localhost");
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
