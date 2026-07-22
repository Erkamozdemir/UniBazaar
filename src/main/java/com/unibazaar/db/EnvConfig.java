package com.unibazaar.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnvConfig {
    private static final Map<String, String> ENV = new HashMap<>();

    static {
        try {
            Path envFile = Paths.get(".env");
            if (Files.exists(envFile)) {
                for (String line : Files.readAllLines(envFile)) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        ENV.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    public static String get(String key, String defaultValue) {
        if (ENV.containsKey(key)) return ENV.get(key);
        String sysEnv = System.getenv(key);
        return sysEnv != null ? sysEnv : defaultValue;
    }
}
