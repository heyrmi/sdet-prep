package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.framework.web.utils.WaitUtils;

public class FramesPage extends BasePage {

    private final By iframeElement = By.id("mce_0_ifr");
    private final By iframeBody = By.id("tinymce");

    @Step("Open iFrame page")
    public FramesPage openIFrame() {
        navigateToPath(Endpoints.IFRAME);
        return this;
    }

    @Step("Get iFrame text content")
    public String getIFrameText() {
        WaitUtils.waitForVisible(iframeElement);
        switchToFrame(iframeElement);
        String text = getText(iframeBody);
        switchToDefaultContent();
        return text;
    }

    @Step("Type '{text}' in iFrame")
    public FramesPage typeInIFrame(String text) {
        WaitUtils.waitForVisible(iframeElement);
        switchToFrame(iframeElement);
        WebElement body = WaitUtils.waitForVisible(iframeBody);
        body.clear();
        body.sendKeys(text);
        switchToDefaultContent();
        return this;
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(iframeElement);
    }
}
