package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;

public class JavaScriptAlertsPage extends BasePage {

    private final By jsAlertButton = By.cssSelector("button[onclick='jsAlert()']");
    private final By jsConfirmButton = By.cssSelector("button[onclick='jsConfirm()']");
    private final By jsPromptButton = By.cssSelector("button[onclick='jsPrompt()']");
    private final By resultText = By.id("result");

    @Step("Open JavaScript Alerts page")
    public JavaScriptAlertsPage open() {
        navigateToPath(Endpoints.JS_ALERTS);
        return this;
    }

    @Step("Click JS Alert button")
    public JavaScriptAlertsPage clickJSAlert() {
        click(jsAlertButton);
        return this;
    }

    @Step("Click JS Confirm button")
    public JavaScriptAlertsPage clickJSConfirm() {
        click(jsConfirmButton);
        return this;
    }

    @Step("Click JS Prompt button")
    public JavaScriptAlertsPage clickJSPrompt() {
        click(jsPromptButton);
        return this;
    }

    @Step("Accept alert and get text")
    public String acceptAlertAndGetText() {
        return acceptAlert();
    }

    @Step("Dismiss alert and get text")
    public String dismissAlertAndGetText() {
        return dismissAlert();
    }

    @Step("Type '{text}' in prompt and accept")
    public JavaScriptAlertsPage typeInPrompt(String text) {
        typeInPromptAndAccept(text);
        return this;
    }

    public String getResultText() {
        return getText(resultText);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(jsAlertButton);
    }
}
