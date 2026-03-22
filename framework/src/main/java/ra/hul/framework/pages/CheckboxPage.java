package ra.hul.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ra.hul.framework.config.ConfigManager;

import java.util.List;

public class CheckboxPage extends BasePage {

    private final By checkboxes = By.cssSelector("#checkboxes input[type='checkbox']");

    public CheckboxPage open() {
        navigateTo(ConfigManager.get("base.url") + "/checkboxes");
        return this;
    }

    public boolean isChecked(int index) {
        List<WebElement> boxes = findElements(checkboxes);
        return boxes.get(index).isSelected();
    }

    public CheckboxPage toggleCheckbox(int index) {
        List<WebElement> boxes = findElements(checkboxes);
        boxes.get(index).click();
        return this;
    }

    public int getCheckboxCount() {
        return findElements(checkboxes).size();
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(checkboxes);
    }
}
