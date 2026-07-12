package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;

public class LoginPage extends BasePage {

    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By flashMessage = By.id("flash");

    @Step("Open login page")
    public LoginPage open() {
        navigateToPath(Endpoints.LOGIN);
        return this;
    }

    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        type(usernameField, username);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click login button")
    public LoginPage clickLogin() {
        click(loginButton);
        return this;
    }

    @Step("Login as {username}")
    public LoginPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return this;
    }

    public String getFlashMessage() {
        return getText(flashMessage);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(usernameField);
    }
}
