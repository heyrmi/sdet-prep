package ra.hul.framework.mobile.screens;

import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LoginScreen extends BaseScreen {

    private final By emailField = AppiumBy.accessibilityId("input-email");
    private final By passwordField = AppiumBy.accessibilityId("input-password");
    private final By loginButton = AppiumBy.accessibilityId("button-LOGIN");
    private final By signUpTab = AppiumBy.accessibilityId("button-sign-up-container");
    private final By loginTab = AppiumBy.accessibilityId("button-login-container");

    // Success dialog (Android)
    private final By successTitle = AppiumBy.id("android:id/alertTitle");
    private final By successMessage = AppiumBy.id("android:id/message");
    private final By okButton = AppiumBy.id("android:id/button1");

    @Step("Navigate to Login tab within Login screen")
    public LoginScreen selectLoginTab() {
        tap(loginTab);
        return this;
    }

    @Step("Navigate to Sign Up tab")
    public LoginScreen selectSignUpTab() {
        tap(signUpTab);
        return this;
    }

    @Step("Enter email: {email}")
    public LoginScreen enterEmail(String email) {
        type(emailField, email);
        hideKeyboard();
        return this;
    }

    @Step("Enter password")
    public LoginScreen enterPassword(String password) {
        type(passwordField, password);
        hideKeyboard();
        return this;
    }

    @Step("Click login button")
    public LoginScreen clickLogin() {
        tap(loginButton);
        return this;
    }

    @Step("Login with email: {email}")
    public LoginScreen loginAs(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
        return this;
    }

    public String getSuccessMessage() {
        return getText(successMessage);
    }

    @Step("Dismiss success dialog")
    public LoginScreen dismissSuccessDialog() {
        tap(okButton);
        return this;
    }

    public boolean isSuccessDialogDisplayed() {
        return waitUntilDisplayed(okButton);
    }

    @Override
    public boolean isLoaded() {
        return waitUntilDisplayed(emailField);
    }
}
