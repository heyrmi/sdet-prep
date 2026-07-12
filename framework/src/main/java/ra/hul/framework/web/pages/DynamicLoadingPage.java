package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.framework.web.utils.WaitUtils;

public class DynamicLoadingPage extends BasePage {

    private final By startButton = By.cssSelector("#start button");
    private final By loadingIndicator = By.id("loading");
    private final By finishText = By.cssSelector("#finish h4");

    @Step("Open dynamic loading example {exampleNumber}")
    public DynamicLoadingPage open(int exampleNumber) {
        navigateToPath(Endpoints.DYNAMIC_LOADING + "/" + exampleNumber);
        return this;
    }

    @Step("Click start button")
    public DynamicLoadingPage clickStart() {
        click(startButton);
        return this;
    }

    @Step("Wait for loading to complete and get result")
    public String waitForResult() {
        WaitUtils.waitForInvisible(loadingIndicator, 30);
        return getText(finishText);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(startButton);
    }
}
