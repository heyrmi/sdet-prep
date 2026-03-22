package ra.hul.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ra.hul.framework.constants.FrameworkConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Singleton config manager that supports environment based override
 * <p>
 * Design Patter: Singleton (eager), Strategy (env-based loading)
 * <p>
 * Usage: ConfigManage.get("browser")
 * Override at runtime: -Dbrowser=firefox
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final Properties properties = new Properties();

    // Static block - loads once when the class is loaded
    static {
        try {
            // 1. Load base config
            properties.load(new FileInputStream(FrameworkConstants.CONFIG_PATH));

            // 2. Overlay env specific config if -Denv is set
            String env = System.getProperty("env");
            if (env != null && !env.isEmpty()) {
                String envConfigPath = FrameworkConstants.CONFIG_PATH
                        .replace(".properties", "-" + env + ".properties");
                properties.load(new FileInputStream(envConfigPath));
                log.info("Load environment config: {}", envConfigPath);
            }
        } catch (IOException e) {
            log.error("Failed to load config file: {}", e.getMessage());
            throw new RuntimeException("Config loading failed", e);
        }
    }

    // Avoid Instantiation
    private ConfigManager() {
    }

    public static String get(String key) {
        // System property gets highest priority (for CI/CD environment)
        String systemProp = System.getProperty(key);
        if (systemProp != null) return systemProp;
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }
}
