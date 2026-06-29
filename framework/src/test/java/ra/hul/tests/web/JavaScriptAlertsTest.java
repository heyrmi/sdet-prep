package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.JavaScriptAlertsPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("JavaScript Alerts")
public class JavaScriptAlertsTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Accept a JS alert and verify result text")
    @Severity(SeverityLevel.NORMAL)
    @Story("JS Alert")
    public void jsAlert_accept_shouldShowSuccessResult() {
        JavaScriptAlertsPage page = new JavaScriptAlertsPage().open();
        page.clickJSAlert();
        page.acceptAlertAndGetText();
        Assert.assertEquals(page.getResultText(), "You successfully clicked an alert");
    }

    @Test(groups = {"regression"},
          description = "Accept a JS confirm and verify OK result")
    @Severity(SeverityLevel.NORMAL)
    @Story("JS Confirm")
    public void jsConfirm_accept_shouldShowOkResult() {
        JavaScriptAlertsPage page = new JavaScriptAlertsPage().open();
        page.clickJSConfirm();
        page.acceptAlertAndGetText();
        Assert.assertEquals(page.getResultText(), "You clicked: Ok");
    }

    @Test(groups = {"regression"},
          description = "Dismiss a JS confirm and verify Cancel result")
    @Severity(SeverityLevel.NORMAL)
    @Story("JS Confirm")
    public void jsConfirm_dismiss_shouldShowCancelResult() {
        JavaScriptAlertsPage page = new JavaScriptAlertsPage().open();
        page.clickJSConfirm();
        page.dismissAlertAndGetText();
        Assert.assertEquals(page.getResultText(), "You clicked: Cancel");
    }

    @Test(groups = {"regression"},
          description = "Type into JS prompt and verify typed text in result")
    @Severity(SeverityLevel.NORMAL)
    @Story("JS Prompt")
    public void jsPrompt_typeAndAccept_shouldShowTypedText() {
        JavaScriptAlertsPage page = new JavaScriptAlertsPage().open();
        page.clickJSPrompt();
        page.typeInPrompt("Hello Allure");
        Assert.assertEquals(page.getResultText(), "You entered: Hello Allure");
    }
}
