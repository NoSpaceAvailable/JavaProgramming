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

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
