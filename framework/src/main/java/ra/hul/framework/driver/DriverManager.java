package ra.hul.framework.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import ra.hul.framework.config.ConfigManager;
import ra.hul.framework.constants.FrameworkConstants;

import java.time.Duration;

/**
 * Thread-safe driver management using ThreadLocal + Factory pattern.
 * <p>
 * Design Patterns:
 * - ThreadLocal ensures each parallel thread gets its own driver instance
 * - Factory Method for browser creation
 * - Singleton-like access via static methods
 * <p>
 * Why ThreadLocal instead of Singleton?
 * Singleton = 1 driver for entire JVM → breaks parallel execution
 * ThreadLocal = 1 driver per thread → safe parallel execution
 */

public class DriverManager {
    private static final Logger log = LogManager.getLogger(DriverManager.class);

    // ThreadLocal stores one WebDriver per thread
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {
    }

    /**
     * Initialize and return a WebDriver for the current thread
     */
    public static WebDriver initDriver() {
        if (driverThreadLocal.get() != null) {
            log.warn("Driver for this thread already exists, returning existing thread");
            return driverThreadLocal.get();
        }

        String browser = ConfigManager.get("browser").toLowerCase();
        boolean headless = ConfigManager.getBoolean("headless");
        int pageLoadTimeout = ConfigManager.getInt("page.load.timeout");

        WebDriver driver = createDriver(browser, headless);

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        driver.manage().window().maximize();

        driverThreadLocal.set(driver);
        return driver;
    }

    /**
     * Factory method - creates the appropriate driver based on browser type
     */
    private static WebDriver createDriver(String browser, Boolean headless) {
        switch (browser) {
            case FrameworkConstants.CHROME -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) options.addArguments("--headless=new");
                options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                // Selenium 4: Enable CDP for network interception, performance logs
                options.addArguments("--remote-allow-origins=*");
                return new ChromeDriver(options);
            }
            case FrameworkConstants.EDGE -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) options.addArguments("--headless=new");
                return new EdgeDriver(options);
            }
            case FrameworkConstants.FIREFOX -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) options.addArguments("--headless");
                return new FirefoxDriver(options);
            }
            default -> throw new IllegalArgumentException("Unsupported Browser: " + browser);
        }
    }

    /**
     * Get driver for current thread
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("Driver not initialized, call initDriver() first");
        }
        return driver;
    }

    /**
     * Quit driver and clean up ThreadLocal to prevent memory leaks
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove(); // prevents memory leak
            log.info("Driver quit on thread: {}", Thread.currentThread().threadId());
        }
    }
}
