package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.framework.web.utils.WaitUtils;

public class MultipleWindowsPage extends BasePage {

    private final By clickHereLink = By.linkText("Click Here");
    private final By newWindowHeading = By.tagName("h3");

    @Step("Open multiple windows page")
    public MultipleWindowsPage open() {
        navigateToPath(Endpoints.WINDOWS);
        return this;
    }

    @Step("Click 'Click Here' to open new window")
    public MultipleWindowsPage clickHere() {
        click(clickHereLink);
        return this;
    }

    @Step("Switch to new window and get heading")
    public String getNewWindowHeading() {
        switchToWindow(1);
        String heading = getText(newWindowHeading);
        return heading;
    }

    @Step("Switch back to original window")
    public MultipleWindowsPage switchToOriginalWindow() {
        switchToWindow(0);
        return this;
    }

    public int getOpenWindowCount() {
        return getWindowCount();
    }

    public String getURL() {
        return getCurrentURL();
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(clickHereLink);
    }
}
