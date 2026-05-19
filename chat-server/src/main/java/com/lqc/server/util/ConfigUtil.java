package com.lqc.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = ConfigUtil.class.getResourceAsStream("/server.properties")) {
            if (is != null) {
                properties.load(is);
                logger.info("Loaded server.properties");
            } else {
                logger.warn("server.properties not found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load server.properties", e);
        }
    }

    private ConfigUtil() {}

    /**
     * Resolves a config value. An environment variable takes precedence over the
     * bundled server.properties, so Docker/containerized deployments can inject
     * credentials without rebuilding the image. The env var name is the key
     * upper-cased with dots replaced by underscores (e.g. {@code db.url} -> {@code DB_URL}).
     */
    public static String get(String key, String defaultValue) {
        String env = System.getenv(toEnvKey(key));
        if (env != null && !env.isBlank()) {
            return env;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_');
    }
}
