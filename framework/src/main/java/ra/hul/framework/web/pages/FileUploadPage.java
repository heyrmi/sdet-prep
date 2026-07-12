package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;

public class FileUploadPage extends BasePage {

    private final By fileInput = By.id("file-upload");
    private final By uploadButton = By.id("file-submit");
    private final By uploadedFileName = By.id("uploaded-files");

    @Step("Open file upload page")
    public FileUploadPage open() {
        navigateToPath(Endpoints.FILE_UPLOAD);
        return this;
    }

    @Step("Choose file: {filePath}")
    public FileUploadPage chooseFile(String filePath) {
        uploadFile(fileInput, filePath);
        return this;
    }

    @Step("Click upload button")
    public FileUploadPage clickUpload() {
        click(uploadButton);
        return this;
    }

    public String getUploadedFileName() {
        return getText(uploadedFileName);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(fileInput);
    }
}
