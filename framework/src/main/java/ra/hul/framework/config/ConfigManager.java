package ra.hul.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ra.hul.framework.constants.FrameworkConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for configuration management with environment-based overrides.
 * <p>
 * Priority order (highest → lowest):
 * 1. System properties (-Dbrowser=firefox)
 * 2. Environment config (config-dev.properties via -Denv=dev)
 * 3. Base config (config.properties)
 * <p>
 * Usage: ConfigManager.get("browser")
 * Override at runtime: -Dbrowser=firefox
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final Properties properties = new Properties();

    // Static block - loads once when the class is loaded
    static {
        try {
            // 1. Load base config
            try (FileInputStream fis = new FileInputStream(FrameworkConstants.CONFIG_PATH)) {
                properties.load(fis);
            }

            // 2. Overlay env specific config if -Denv is set
            String env = System.getProperty("env");
            if (env != null && !env.isEmpty()) {
                String envConfigPath = FrameworkConstants.CONFIG_PATH
                        .replace(".properties", "-" + env + ".properties");
                try (FileInputStream fis = new FileInputStream(envConfigPath)) {
                    properties.load(fis);
                }
                log.info("Loaded environment config: {}", envConfigPath);
            }
        } catch (IOException e) {
            log.error("Failed to load config file: {}", e.getMessage());
            throw new RuntimeException("Config loading failed", e);
        }
    }

    // Prevent instantiation
    private ConfigManager() {
    }

    /**
     * Get a config value. Fails fast if the key is missing.
     * System properties take highest priority (for CI/CD overrides).
     */
    public static String get(String key) {
        String systemProp = System.getProperty(key);
        if (systemProp != null) return systemProp;
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Missing config key: '" + key + "' — add it to config.properties or pass -D" + key + "=value");
        }
        return value;
    }

    /**
     * Get a config value with a fallback default. Use when the key is optional.
     */
    public static String getOrDefault(String key, String defaultValue) {
        String systemProp = System.getProperty(key);
        if (systemProp != null) return systemProp;
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static int getIntOrDefault(String key, int defaultValue) {
        String value = getOrDefault(key, null);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }
}
