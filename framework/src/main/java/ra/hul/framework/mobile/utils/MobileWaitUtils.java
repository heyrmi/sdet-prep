package ra.hul.framework.mobile.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import ra.hul.framework.core.constants.TimeoutConstants;
import ra.hul.framework.mobile.driver.AppiumDriverManager;

import java.time.Duration;

public final class MobileWaitUtils {

    private MobileWaitUtils() {
    }

    public static WebElement waitForVisible(By locator) {
        return waitForVisible(locator, TimeoutConstants.EXPLICIT_WAIT);
    }

    public static WebElement waitForVisible(By locator, int timeoutSeconds) {
        return new WebDriverWait(AppiumDriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(By locator) {
        return new WebDriverWait(AppiumDriverManager.getDriver(),
                Duration.ofSeconds(TimeoutConstants.EXPLICIT_WAIT))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForInvisible(By locator, int timeoutSeconds) {
        return new WebDriverWait(AppiumDriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static WebElement fluentWait(By locator) {
        return new FluentWait<>(AppiumDriverManager.getDriver())
                .withTimeout(Duration.ofSeconds(TimeoutConstants.FLUENT_WAIT))
                .pollingEvery(Duration.ofMillis(TimeoutConstants.POLLING_TIME))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(driver -> driver.findElement(locator));
    }
}
