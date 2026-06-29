package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.FramesPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Frames")
public class FramesTest extends BaseWebTest {

    @Test(groups = {"regression"},
            description = "Read text content from an iFrame")
    @Severity(SeverityLevel.NORMAL)
    @Story("iFrame")
    public void iframe_readContent_shouldReturnText() {
        FramesPage page = new FramesPage().openIFrame();
        String iframeText = page.getIFrameText();
        Assert.assertNotNull(iframeText);
        Assert.assertFalse(iframeText.isEmpty(), "iFrame should contain text");
    }

    @Test(groups = {"regression"},
            description = "Type text into iFrame editor", enabled = false)
    @Severity(SeverityLevel.NORMAL)
    @Story("iFrame")
    public void iframe_typeText_shouldUpdateContent() {
        FramesPage page = new FramesPage().openIFrame();
        page.typeInIFrame("Automation test content");
        String text = page.getIFrameText();
        Assert.assertEquals(text, "Automation test content");
    }
}
