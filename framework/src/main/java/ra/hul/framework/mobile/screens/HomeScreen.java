package ra.hul.framework.mobile.screens;

import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Home screen of the WebDriverIO demo app.
 * Bottom navigation: Home, WebView, Login, Forms, Swipe, Drag
 */
public class HomeScreen extends BaseScreen {

    private final By homeTab = AppiumBy.accessibilityId("Home");
    private final By webViewTab = AppiumBy.accessibilityId("Webview");
    private final By loginTab = AppiumBy.accessibilityId("Login");
    private final By formsTab = AppiumBy.accessibilityId("Forms");
    private final By swipeTab = AppiumBy.accessibilityId("Swipe");

    @Step("Navigate to Home tab")
    public HomeScreen navigateToHome() {
        tap(homeTab);
        return this;
    }

    @Step("Navigate to Login tab")
    public LoginScreen navigateToLogin() {
        tap(loginTab);
        return new LoginScreen();
    }

    @Step("Navigate to Forms tab")
    public FormScreen navigateToForms() {
        tap(formsTab);
        return new FormScreen();
    }

    @Step("Navigate to Swipe tab")
    public SwipeScreen navigateToSwipe() {
        tap(swipeTab);
        return new SwipeScreen();
    }

    @Step("Navigate to WebView tab")
    public WebViewScreen navigateToWebView() {
        tap(webViewTab);
        return new WebViewScreen();
    }

    @Override
    public boolean isLoaded() {
        return waitUntilDisplayed(homeTab);
    }
}
