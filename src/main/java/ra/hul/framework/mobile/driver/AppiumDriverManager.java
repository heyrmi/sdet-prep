package ra.hul.framework.mobile.driver;

import io.appium.java_client.AppiumDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ra.hul.framework.core.config.ConfigManager;

/**
 * ThreadLocal-based Appium driver manager for thread-safe parallel execution.
 */
public final class AppiumDriverManager {

    private static final Logger log = LogManager.getLogger(AppiumDriverManager.class);
    private static final ThreadLocal<AppiumDriver> driverThreadLocal = new ThreadLocal<>();

    private AppiumDriverManager() {
    }

    public static void initDriver() {
        if (driverThreadLocal.get() != null) {
            log.warn("Appium driver already exists for thread {}", Thread.currentThread().threadId());
            return;
        }

        String platform = ConfigManager.get("mobile.platform");
        AppiumDriver driver = AppiumDriverFactory.createDriver(platform);
        driverThreadLocal.set(driver);

        log.info("Appium driver initialized: platform={} thread={}",
                platform, Thread.currentThread().threadId());
    }

    public static AppiumDriver getDriver() {
        AppiumDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No Appium driver for thread " + Thread.currentThread().threadId()
                            + " — call initDriver() first");
        }
        return driver;
    }

    public static void quitDriver() {
        AppiumDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
            log.info("Appium driver quit on thread {}", Thread.currentThread().threadId());
        }
    }
}
