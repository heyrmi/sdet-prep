package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import ra.hul.framework.core.constants.Endpoints;

public class FileDownloadPage extends BasePage {

    @Step("Open file download page")
    public FileDownloadPage open() {
        navigateToPath(Endpoints.FILE_DOWNLOAD);
        return this;
    }

    @Override
    public boolean isLoaded() {
        return getDriver().getCurrentUrl().contains("/download");
    }
}
