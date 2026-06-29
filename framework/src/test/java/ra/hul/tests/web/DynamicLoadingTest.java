package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.DynamicLoadingPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Dynamic Content")
public class DynamicLoadingTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Wait for hidden element to appear after clicking start")
    @Severity(SeverityLevel.NORMAL)
    @Story("Dynamic Loading")
    public void dynamicLoading_example1_shouldShowHiddenElement() {
        DynamicLoadingPage page = new DynamicLoadingPage();
        page.open(1).clickStart();
        String result = page.waitForResult();
        Assert.assertEquals(result, "Hello World!");
    }

    @Test(groups = {"regression"},
          description = "Wait for element to be rendered after clicking start")
    @Severity(SeverityLevel.NORMAL)
    @Story("Dynamic Loading")
    public void dynamicLoading_example2_shouldRenderElement() {
        DynamicLoadingPage page = new DynamicLoadingPage();
        page.open(2).clickStart();
        String result = page.waitForResult();
        Assert.assertEquals(result, "Hello World!");
    }
}
