package ra.hul.framework.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;
import ra.hul.framework.driver.DriverManager;
import ra.hul.framework.utils.WaitUtils;

/**
 * Base class for all Page Objects.
 * <p>
 * Design Pattern: Template Method — defines the skeleton of page interactions.
 * All pages extend this and inherit common behavior.
 * <p>
 * Why BasePage?
 * - DRY: Common actions (click, type, getText) defined once
 * - Encapsulation: WebDriver interactions wrapped with waits + logging
 * - Resilience: Built-in retry for StaleElementReferenceException
 */
public class BasePage {
    protected final Logger log = LogManager.getLogger(BasePage.class);

    // Core Actions
    protected void click(By locator) {
        log.info("Clicking: {}", locator);
        WaitUtils.waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {
        log.info("Typing '{}' into: {}", text, locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String getText(By locator) {
        return WaitUtils.waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return DriverManager.getDriver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    protected String getAttribute(By locator, String attribute) {
        return WaitUtils.waitForVisible(locator).getAttribute(attribute);
    }

    protected void selectDropdown(By locator, String visibleText) {
        log.info("Selecting '{}' from dropdown: {}", visibleText, locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        new Select(element).selectByVisibleText(visibleText);
    }

    // Navigation
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        DriverManager.getDriver().get(url);
    }

    protected String getPageTitle() {
        return DriverManager.getDriver().getTitle();
    }

    protected String getCurrentURL() {
        return DriverManager.getDriver().getCurrentUrl();
    }

    // JavaScript Executor
    protected Object executeJs(String script, Object... args) {
        return ((JavascriptExecutor) DriverManager.getDriver()).executeScript(script, args);
    }

    // Screenshot
    public static String takesScreenshot(String testName) {
        TakesScreenshot ts = (TakesScreenshot) DriverManager.getDriver();
        return ts.getScreenshotAs(OutputType.BASE64);
    }
}
