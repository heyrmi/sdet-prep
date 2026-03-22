package ra.hul.framework.pages;


import org.openqa.selenium.By;
import ra.hul.framework.config.ConfigManager;

/**
 * Page Object for the Login page.
 * <p>
 * Rules for Page Objects:
 * 1. Each page = 1 class
 * 2. Locators are PRIVATE (encapsulation)
 * 3. Methods return the NEXT page object (fluent navigation)
 * 4. NO assertions in page objects (that's the test's job)
 * 5. NO WebDriver.get() calls — navigation is explicit
 */
public class LoginPage extends BasePage {

    // ---- Locators (private — encapsulation) ----
    private final By usernameInput = By.id("username");
    private final By passwordInput = By.id("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By flashMessage = By.id("flash");

    // ---- Actions (public — user behavior) ----

    public LoginPage open() {
        navigateTo(ConfigManager.get("base.url") + "/login");
        return this;
    }

    public LoginPage enterUsername(String username) {
        type(usernameInput, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(passwordInput, password);
        return this;
    }

    public void clickLogin() {
        click(loginButton);
    }

    public String getFlashMessage() {
        return getText(flashMessage).trim();
    }

    // ---- Compound action ----
    public void loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(loginButton);
    }
}
