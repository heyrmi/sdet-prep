package ra.hul.framework.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import ra.hul.framework.constants.FrameworkConstants;
import ra.hul.framework.driver.DriverManager;

import java.time.Duration;

/**
 * Centralised wait utilities
 * Rule: Never use Thread.sleep() or implicit waits
 * Always use explicit waits - they are predictable and element specific
 */
public class WaitUtils {
    // To avoid instantiation
    private WaitUtils() {
    }

    private static final int WAIT_TIME = FrameworkConstants.EXPLICIT_WAIT;

    public static WebElement waitForVisible(By locator) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(WAIT_TIME))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(WAIT_TIME))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForInvisible(By locator) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(WAIT_TIME))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Fluent wait - useful for polling dynamic elements (spinner, animations)
     * Polls every POLLING_TIME
     */
    public static WebElement fluentWait(By locator) {
        return new FluentWait<>(DriverManager.getDriver())
                .withTimeout(Duration.ofSeconds(FrameworkConstants.FLUENT_WAIT))
                .pollingEvery(Duration.ofMillis(FrameworkConstants.FLUENT_WAIT_POLL))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(driver -> driver.findElement(locator));
    }
}