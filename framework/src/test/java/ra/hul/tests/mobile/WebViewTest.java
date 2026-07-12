package ra.hul.tests.mobile;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.mobile.screens.HomeScreen;
import ra.hul.framework.mobile.screens.WebViewScreen;
import ra.hul.tests.base.BaseMobileTest;

@Epic("Mobile Automation")
@Feature("WebView")
public class WebViewTest extends BaseMobileTest {

    @Test(groups = {"regression"},
          description = "Navigate to WebView screen")
    @Severity(SeverityLevel.NORMAL)
    @Story("WebView Navigation")
    public void webView_navigate_shouldLoadWebViewScreen() {
        HomeScreen home = new HomeScreen();
        WebViewScreen webViewScreen = home.navigateToWebView();
        Assert.assertTrue(webViewScreen.isLoaded(), "WebView screen should be loaded");
    }

    @Test(groups = {"regression"},
          description = "Switch between native and webview contexts")
    @Severity(SeverityLevel.NORMAL)
    @Story("Context Switching")
    public void webView_switchContext_shouldToggleBetweenNativeAndWeb() {
        HomeScreen home = new HomeScreen();
        WebViewScreen webViewScreen = home.navigateToWebView();

        // Verify starting in native context
        Assert.assertTrue(webViewScreen.getCurrentContext().contains("NATIVE"),
                "Should start in native context");

        if (webViewScreen.isWebViewAvailable()) {
            webViewScreen.switchToWebView();
            Assert.assertTrue(webViewScreen.getCurrentContext().contains("WEBVIEW"),
                    "Should be in webview context after switch");

            webViewScreen.switchToNative();
            Assert.assertTrue(webViewScreen.getCurrentContext().contains("NATIVE"),
                    "Should return to native context");
        }
    }
}
