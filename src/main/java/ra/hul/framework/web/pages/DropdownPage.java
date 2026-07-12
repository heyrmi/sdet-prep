package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;

public class DropdownPage extends BasePage {

    private final By dropdown = By.id("dropdown");

    @Step("Open dropdown page")
    public DropdownPage open() {
        navigateToPath(Endpoints.DROPDOWN);
        return this;
    }

    @Step("Select option: {visibleText}")
    public DropdownPage selectOption(String visibleText) {
        selectDropdown(dropdown, visibleText);
        return this;
    }

    @Step("Select option by value: {value}")
    public DropdownPage selectByValue(String value) {
        selectDropdownByValue(dropdown, value);
        return this;
    }

    public String getSelectedOption() {
        return getSelectedDropdownText(dropdown);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(dropdown);
    }
}
