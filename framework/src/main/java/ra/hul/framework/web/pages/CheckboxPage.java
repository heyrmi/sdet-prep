package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ra.hul.framework.core.constants.Endpoints;

import java.util.List;

public class CheckboxPage extends BasePage {

    private final By checkboxes = By.cssSelector("#checkboxes input[type='checkbox']");

    @Step("Open checkboxes page")
    public CheckboxPage open() {
        navigateToPath(Endpoints.CHECKBOXES);
        return this;
    }

    @Step("Toggle checkbox at index: {index}")
    public CheckboxPage toggleCheckbox(int index) {
        List<WebElement> elements = findElements(checkboxes);
        elements.get(index).click();
        return this;
    }

    public boolean isChecked(int index) {
        List<WebElement> elements = findElements(checkboxes);
        return elements.get(index).isSelected();
    }

    public int getCheckboxCount() {
        return findElements(checkboxes).size();
    }

    @Override
    public boolean isLoaded() {
        return !findElements(checkboxes).isEmpty();
    }
}
