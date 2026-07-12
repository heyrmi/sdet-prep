package ra.hul.framework.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration manager with 4-level property resolution.
 * Precedence: Environment Variables > System Properties > Environment Config > Default Config
 */
public final class ConfigManager {

    private static final Properties properties = new Properties();

    static {
        loadProperties("config.properties");

        String env = System.getProperty("env");
        if (env == null) {
            env = System.getenv("ENV");
        }
        if (env != null && !env.isBlank()) {
            loadProperties("config-" + env + ".properties");
        }
    }

    private ConfigManager() {
    }

    private static void loadProperties(String fileName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(fileName)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + fileName, e);
        }
    }

    /**
     * Resolve a config value with 4-level precedence:
     * 1. OS environment variable (dot.key converted to SCREAMING_SNAKE_CASE)
     * 2. JVM system property (-Dkey=value)
     * 3. Environment-specific properties file (overlaid on default)
     * 4. Default config.properties
     */
    public static String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null) return envValue;

        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;

        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing config key: " + key);
        }
        return value;
    }

    public static String getOrDefault(String key, String defaultValue) {
        try {
            return get(key);
        } catch (IllegalStateException e) {
            return defaultValue;
        }
    }

    public static int getIntOrDefault(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static long getLongOrDefault(String key, long defaultValue) {
        try {
            return Long.parseLong(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
