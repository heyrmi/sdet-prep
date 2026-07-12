package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class SecurePage extends BasePage {

    private final By flashMessage = By.id("flash");
    private final By logoutButton = By.cssSelector("a[href='/logout']");
    private final By heading = By.tagName("h2");

    public String getFlashMessage() {
        return getText(flashMessage);
    }

    public String getHeading() {
        return getText(heading);
    }

    public boolean isLogoutButtonDisplayed() {
        return isDisplayed(logoutButton);
    }

    @Step("Click logout")
    public void clickLogout() {
        click(logoutButton);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(logoutButton);
    }
}
