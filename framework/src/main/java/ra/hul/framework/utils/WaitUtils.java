package ra.hul.framework.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import ra.hul.framework.config.ConfigManager;
import ra.hul.framework.driver.DriverManager;

import java.time.Duration;

/**
 * Centralized wait utilities — all timeouts driven from config.properties.
 * Rule: Never use Thread.sleep() or implicit waits.
 * Always use explicit waits — they are predictable and element-specific.
 */
public class WaitUtils {

    private WaitUtils() {
    }

    // ---- Explicit Waits (default timeout from config) ----

    public static WebElement waitForVisible(By locator) {
        return waitForVisible(locator, ConfigManager.getInt("explicit.wait"));
    }

    public static WebElement waitForVisible(By locator, int timeoutSeconds) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return waitForClickable(locator, ConfigManager.getInt("explicit.wait"));
    }

    public static WebElement waitForClickable(By locator, int timeoutSeconds) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForInvisible(By locator) {
        return waitForInvisible(locator, ConfigManager.getInt("explicit.wait"));
    }

    public static boolean waitForInvisible(By locator, int timeoutSeconds) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Fluent wait — useful for polling dynamic elements (spinners, animations).
     * Polls every polling.time ms, ignores stale/missing element exceptions.
     */
    public static WebElement fluentWait(By locator) {
        return new FluentWait<>(DriverManager.getDriver())
                .withTimeout(Duration.ofSeconds(ConfigManager.getInt("fluent.wait")))
                .pollingEvery(Duration.ofMillis(ConfigManager.getInt("polling.time")))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(driver -> driver.findElement(locator));
    }
}
