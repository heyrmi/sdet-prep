package ra.hul.framework.mobile.screens;

import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class FormScreen extends BaseScreen {

    private final By textInput = AppiumBy.accessibilityId("text-input");
    private final By inputResult = AppiumBy.accessibilityId("input-text-result");
    private final By switchToggle = AppiumBy.accessibilityId("switch");
    private final By switchText = AppiumBy.accessibilityId("switch-text");

    @Step("Type '{text}' into text input")
    public FormScreen typeInInput(String text) {
        type(textInput, text);
        hideKeyboard();
        return this;
    }

    public String getInputResult() {
        return getText(inputResult);
    }

    @Step("Toggle switch")
    public FormScreen toggleSwitch() {
        tap(switchToggle);
        return this;
    }

    public String getSwitchText() {
        return getText(switchText);
    }

    @Override
    public boolean isLoaded() {
        return waitUntilDisplayed(textInput);
    }
}
