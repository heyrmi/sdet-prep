package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.HoverPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Hover Actions")
public class HoverTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Hover over first profile to reveal name")
    @Severity(SeverityLevel.NORMAL)
    @Story("Hover")
    public void hover_firstProfile_shouldShowName() {
        HoverPage page = new HoverPage().open();
        String name = page.getProfileName(0);
        Assert.assertNotNull(name);
        Assert.assertTrue(name.contains("name: user1"));
    }

    @Test(groups = {"regression"},
          description = "Hover over second profile to reveal name")
    @Severity(SeverityLevel.MINOR)
    @Story("Hover")
    public void hover_secondProfile_shouldShowName() {
        HoverPage page = new HoverPage().open();
        String name = page.getProfileName(1);
        Assert.assertTrue(name.contains("name: user2"));
    }

    @Test(groups = {"regression"},
          description = "Hover over third profile to reveal name")
    @Severity(SeverityLevel.MINOR)
    @Story("Hover")
    public void hover_thirdProfile_shouldShowName() {
        HoverPage page = new HoverPage().open();
        String name = page.getProfileName(2);
        Assert.assertTrue(name.contains("name: user3"));
    }
}
