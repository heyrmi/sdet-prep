package ra.hul.framework.mobile.screens;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.remote.SupportsContextSwitching;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.util.Set;

public class WebViewScreen extends BaseScreen {

    private final By webViewTitle = AppiumBy.accessibilityId("Webview-screen");

    private SupportsContextSwitching contextSwitcher() {
        return (SupportsContextSwitching) getDriver();
    }

    @Step("Switch to WebView context")
    public WebViewScreen switchToWebView() {
        Set<String> contexts = contextSwitcher().getContextHandles();
        for (String context : contexts) {
            if (context.contains("WEBVIEW")) {
                contextSwitcher().context(context);
                break;
            }
        }
        return this;
    }

    @Step("Switch back to Native context")
    public WebViewScreen switchToNative() {
        contextSwitcher().context("NATIVE_APP");
        return this;
    }

    @Step("Get current context")
    public String getCurrentContext() {
        return contextSwitcher().getContext();
    }

    public boolean isWebViewAvailable() {
        Set<String> contexts = contextSwitcher().getContextHandles();
        return contexts.stream().anyMatch(c -> c.contains("WEBVIEW"));
    }

    @Override
    public boolean isLoaded() {
        return isWebViewAvailable();
    }
}
