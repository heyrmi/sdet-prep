package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.MultipleWindowsPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Multiple Windows")
public class MultipleWindowsTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Open new window and verify heading")
    @Severity(SeverityLevel.NORMAL)
    @Story("Window Handling")
    public void multipleWindows_openNew_shouldShowNewWindowHeading() {
        MultipleWindowsPage page = new MultipleWindowsPage().open();
        page.clickHere();

        Assert.assertEquals(page.getOpenWindowCount(), 2, "Should have 2 windows open");

        String heading = page.getNewWindowHeading();
        Assert.assertEquals(heading, "New Window");
    }

    @Test(groups = {"regression"},
          description = "Switch back to original window after opening new one")
    @Severity(SeverityLevel.MINOR)
    @Story("Window Handling")
    public void multipleWindows_switchBack_shouldReturnToOriginal() {
        MultipleWindowsPage page = new MultipleWindowsPage().open();
        page.clickHere();
        page.switchToOriginalWindow();

        Assert.assertTrue(page.getURL().contains("/windows"));
    }
}
