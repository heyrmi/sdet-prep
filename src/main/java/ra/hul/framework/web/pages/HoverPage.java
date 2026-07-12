package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ra.hul.framework.core.constants.Endpoints;

import java.util.List;

public class HoverPage extends BasePage {

    private final By figures = By.cssSelector(".figure");
    private final By figureCaption = By.cssSelector(".figcaption h5");

    @Step("Open hovers page")
    public HoverPage open() {
        navigateToPath(Endpoints.HOVERS);
        return this;
    }

    @Step("Hover over profile at index: {index}")
    public HoverPage hoverOverProfile(int index) {
        hoverOver(By.cssSelector(".figure:nth-of-type(" + (index + 1) + ") img"));
        return this;
    }

    @Step("Get profile name at index: {index}")
    public String getProfileName(int index) {
        hoverOverProfile(index);
        List<WebElement> captions = findElements(figureCaption);
        return captions.get(index).getText();
    }

    @Override
    public boolean isLoaded() {
        return !findElements(figures).isEmpty();
    }
}
