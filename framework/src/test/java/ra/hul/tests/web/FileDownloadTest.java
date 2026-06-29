package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.FileDownloadPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("File Download")
public class FileDownloadTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Verify file download page loads and has download links")
    @Severity(SeverityLevel.NORMAL)
    @Story("File Download")
    public void fileDownload_pageLoad_shouldShowDownloadLinks() {
        FileDownloadPage page = new FileDownloadPage().open();
        Assert.assertTrue(page.isLoaded(), "File download page should be loaded");
    }
}
