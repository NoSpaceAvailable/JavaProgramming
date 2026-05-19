package com.lqc.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves the server host/port the client should connect to.
 *
 * <p>Resolution order (highest priority first):
 * <ol>
 *     <li>JVM system property, e.g. {@code -Dserver.host=192.168.1.10}</li>
 *     <li>Environment variable {@code SERVER_HOST} / {@code SERVER_PORT}</li>
 *     <li>Bundled {@code client.properties}</li>
 *     <li>Built-in default ({@code localhost:9000})</li>
 * </ol>
 *
 * This lets the same client build be distributed to other users: they only need
 * to point it at the server machine, e.g. {@code mvn javafx:run -Dserver.host=<server-ip>}.
 */
public final class ClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = ClientConfig.class.getResourceAsStream("/client.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (Exception e) {
            logger.warn("Failed to load client.properties, using defaults", e);
        }
    }

    private ClientConfig() {}

    public static String getServerHost() {
        return resolve("server.host", "SERVER_HOST", "localhost");
    }

    public static int getServerPort() {
        String value = resolve("server.port", "SERVER_PORT", "9000");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 9000;
        }
    }

    private static String resolve(String propertyKey, String envKey, String defaultValue) {
        String sys = System.getProperty(propertyKey);
        if (sys != null && !sys.isBlank()) return sys;

        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;

        return properties.getProperty(propertyKey, defaultValue);
    }
}
