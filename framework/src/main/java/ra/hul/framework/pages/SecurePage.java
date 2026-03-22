package ra.hul.framework.pages;

import org.openqa.selenium.By;

/**
 * Page Object for the-internet.herokuapp.com/secure
 * This page appears after successful login.
 */
public class SecurePage extends BasePage {

    private final By flashMessage = By.id("flash");
    private final By logoutButton = By.cssSelector("a[href='/logout']");
    private final By heading = By.tagName("h2");

    public String getFlashMessage() {
        return getText(flashMessage).trim();
    }

    public boolean isLogoutButtonDisplayed() {
        return isDisplayed(logoutButton);
    }

    public String getHeading() {
        return getText(heading);
    }

    public LoginPage clickLogout() {
        click(logoutButton);
        return new LoginPage();
    }
}