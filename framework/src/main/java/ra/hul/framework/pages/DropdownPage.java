package ra.hul.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import ra.hul.framework.config.ConfigManager;
import ra.hul.framework.utils.WaitUtils;

public class DropdownPage extends BasePage {

    private final By dropdown = By.id("dropdown");

    public DropdownPage open() {
        navigateTo(ConfigManager.get("base.url") + "/dropdown");
        return this;
    }

    public DropdownPage selectOption(String visibleText) {
        selectDropdown(dropdown, visibleText);
        return this;
    }

    public String getSelectedOption() {
        WebElement element = WaitUtils.waitForVisible(dropdown);
        return new Select(element).getFirstSelectedOption().getText();
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(dropdown);
    }
}