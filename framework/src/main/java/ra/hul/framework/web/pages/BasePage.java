package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.web.driver.DriverManager;
import ra.hul.framework.web.utils.WaitUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for all page objects.
 * Every interaction uses explicit waits to reduce flakiness.
 * Methods are annotated with @Step for Allure reporting.
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());

    public abstract boolean isLoaded();

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    // ---- Core interactions ----

    @Step("Click element: {locator}")
    protected void click(By locator) {
        log.info("Clicking: {}", locator);
        WaitUtils.waitForClickable(locator).click();
    }

    @Step("Type '{text}' into: {locator}")
    protected void type(By locator, String text) {
        log.info("Typing into: {}", locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    @Step("Get text from: {locator}")
    protected String getText(By locator) {
        return WaitUtils.waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return getDriver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Step("Get attribute '{attribute}' from: {locator}")
    protected String getAttribute(By locator, String attribute) {
        return WaitUtils.waitForVisible(locator).getAttribute(attribute);
    }

    @Step("Select dropdown option: {visibleText}")
    protected void selectDropdown(By locator, String visibleText) {
        log.info("Selecting '{}' from: {}", visibleText, locator);
        new Select(WaitUtils.waitForVisible(locator)).selectByVisibleText(visibleText);
    }

    @Step("Select dropdown by value: {value}")
    protected void selectDropdownByValue(By locator, String value) {
        new Select(WaitUtils.waitForVisible(locator)).selectByValue(value);
    }

    protected String getSelectedDropdownText(By locator) {
        return new Select(WaitUtils.waitForVisible(locator))
                .getFirstSelectedOption().getText();
    }

    protected List<WebElement> findElements(By locator) {
        return getDriver().findElements(locator);
    }

    // ---- Navigation ----

    @Step("Navigate to: {url}")
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        getDriver().get(url);
    }

    protected void navigateToPath(String path) {
        navigateTo(ConfigManager.get("base.url") + path);
    }

    protected String getPageTitle() {
        return getDriver().getTitle();
    }

    protected String getCurrentURL() {
        return getDriver().getCurrentUrl();
    }

    // ---- JavaScript ----

    protected Object executeJs(String script, Object... args) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, args);
    }

    // ---- Alerts ----

    @Step("Accept alert")
    protected String acceptAlert() {
        Alert alert = getDriver().switchTo().alert();
        String text = alert.getText();
        alert.accept();
        return text;
    }

    @Step("Dismiss alert")
    protected String dismissAlert() {
        Alert alert = getDriver().switchTo().alert();
        String text = alert.getText();
        alert.dismiss();
        return text;
    }

    @Step("Type '{text}' into prompt and accept")
    protected void typeInPromptAndAccept(String text) {
        Alert alert = getDriver().switchTo().alert();
        alert.sendKeys(text);
        alert.accept();
    }

    // ---- Frames ----

    @Step("Switch to frame: {locator}")
    protected void switchToFrame(By locator) {
        getDriver().switchTo().frame(getDriver().findElement(locator));
    }

    @Step("Switch to frame by index: {index}")
    protected void switchToFrame(int index) {
        getDriver().switchTo().frame(index);
    }

    @Step("Switch to default content")
    protected void switchToDefaultContent() {
        getDriver().switchTo().defaultContent();
    }

    // ---- Windows ----

    @Step("Switch to window by index: {index}")
    protected void switchToWindow(int index) {
        var handles = new ArrayList<>(getDriver().getWindowHandles());
        getDriver().switchTo().window(handles.get(index));
    }

    protected int getWindowCount() {
        return getDriver().getWindowHandles().size();
    }

    // ---- Actions ----

    @Step("Drag from {source} to {target}")
    protected void dragAndDrop(By source, By target) {
        new Actions(getDriver())
                .dragAndDrop(
                        WaitUtils.waitForVisible(source),
                        WaitUtils.waitForVisible(target))
                .perform();
    }

    @Step("Hover over: {locator}")
    protected void hoverOver(By locator) {
        new Actions(getDriver())
                .moveToElement(WaitUtils.waitForVisible(locator))
                .perform();
    }

    // ---- File Upload ----

    @Step("Upload file: {filePath}")
    protected void uploadFile(By locator, String filePath) {
        getDriver().findElement(locator).sendKeys(filePath);
    }

}
