package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.FileUploadPage;
import ra.hul.tests.base.BaseWebTest;

import java.io.File;

@Epic("Web Automation")
@Feature("File Upload")
public class FileUploadTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Upload a text file and verify the filename is displayed")
    @Severity(SeverityLevel.NORMAL)
    @Story("File Upload")
    public void fileUpload_textFile_shouldShowUploadedName() {
        String filePath = new File("src/test/resources/testdata/upload-test.txt").getAbsolutePath();

        FileUploadPage page = new FileUploadPage().open();
        page.chooseFile(filePath).clickUpload();

        Assert.assertEquals(page.getUploadedFileName().trim(), "upload-test.txt");
    }
}
