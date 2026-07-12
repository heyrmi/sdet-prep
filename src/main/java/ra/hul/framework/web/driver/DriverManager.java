package ra.hul.framework.web.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.core.constants.TimeoutConstants;

import java.time.Duration;

/**
 * ThreadLocal-based WebDriver manager for thread-safe parallel execution.
 * Each thread gets its own isolated WebDriver instance.
 */
public final class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void initDriver() {
        if (driverThreadLocal.get() != null) {
            log.warn("Driver already exists for thread {}", Thread.currentThread().threadId());
            return;
        }

        String browser = ConfigManager.get("browser");
        boolean headless = ConfigManager.getBoolean("headless");

        WebDriver driver = WebDriverFactory.createDriver(browser, headless);
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(TimeoutConstants.PAGE_LOAD_TIMEOUT));
        driver.manage().window().maximize();

        driverThreadLocal.set(driver);
        log.info("Driver initialized: {} (headless={}) thread={}",
                browser, headless, Thread.currentThread().threadId());
    }

    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No driver for thread " + Thread.currentThread().threadId()
                            + " — call initDriver() first");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
            log.info("Driver quit on thread {}", Thread.currentThread().threadId());
        }
    }
}
