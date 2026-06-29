package ra.hul.framework.mobile.screens;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HidesKeyboard;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import ra.hul.framework.mobile.driver.AppiumDriverManager;
import ra.hul.framework.mobile.utils.GestureUtils;
import ra.hul.framework.mobile.utils.MobileWaitUtils;

/**
 * Abstract base for all mobile screen objects.
 * Parallel to BasePage but uses Appium-specific patterns.
 */
public abstract class BaseScreen {

    protected final Logger log = LogManager.getLogger(getClass());

    public abstract boolean isLoaded();

    protected AppiumDriver getDriver() {
        return AppiumDriverManager.getDriver();
    }

    @Step("Tap element: {locator}")
    protected void tap(By locator) {
        log.info("Tapping: {}", locator);
        MobileWaitUtils.waitForClickable(locator).click();
    }

    @Step("Type '{text}' into: {locator}")
    protected void type(By locator, String text) {
        log.info("Typing into: {}", locator);
        WebElement element = MobileWaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    @Step("Get text from: {locator}")
    protected String getText(By locator) {
        return MobileWaitUtils.waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return getDriver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Step("Hide keyboard")
    protected void hideKeyboard() {
        try {
            ((HidesKeyboard) getDriver()).hideKeyboard();
        } catch (Exception ignored) {
            // Keyboard might not be visible
        }
    }

    @Step("Swipe left")
    protected void swipeLeft() {
        GestureUtils.swipeLeft(getDriver());
    }

    @Step("Swipe right")
    protected void swipeRight() {
        GestureUtils.swipeRight(getDriver());
    }

    @Step("Scroll down")
    protected void scrollDown() {
        GestureUtils.scrollDown(getDriver());
    }

    @Step("Scroll up")
    protected void scrollUp() {
        GestureUtils.scrollUp(getDriver());
    }

    /**
     * Waits for an element to become visible (up to the configured explicit wait timeout).
     * Use this in isLoaded() checks where a screen transition may still be in progress.
     */
    protected boolean waitUntilDisplayed(By locator) {
        try {
            MobileWaitUtils.waitForVisible(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
