package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.LoginPage;
import ra.hul.framework.web.pages.SecurePage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Authentication")
public class LoginTest extends BaseWebTest {

    @Test(groups = {"smoke", "regression"},
            description = "Verify successful login with valid credentials")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Validates that a user can log in with valid credentials and sees the secure area")
    @Story("Login")
    public void login_validCredentials_shouldShowSecurePage() {
        LoginPage loginPage = new LoginPage();
        loginPage.open()
                .enterUsername("tomsmith")
                .enterPassword("SuperSecretPassword!")
                .clickLogin();

        SecurePage securePage = new SecurePage();
        Assert.assertTrue(securePage.getFlashMessage().contains("You logged into a secure area!"));
        Assert.assertTrue(securePage.isLogoutButtonDisplayed());
    }

    @Test(groups = {"regression"},
            description = "Verify logout after successful login")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Logout")
    public void login_thenLogout_shouldReturnToLoginPage() {
        LoginPage loginPage = new LoginPage();
        loginPage.open().loginAs("tomsmith", "SuperSecretPassword!");

        SecurePage securePage = new SecurePage();
        securePage.clickLogout();

        Assert.assertTrue(loginPage.getFlashMessage().contains("You logged out of the secure area!"));
    }

    @Test(groups = {"regression"},
            description = "Data-driven: verify error messages for invalid credentials",
            dataProvider = "invalidCredentials")
    @Severity(SeverityLevel.NORMAL)
    @Story("Login Validation")
    public void login_invalidData_shouldShowError(String user, String pass, String expectedMsg) {
        LoginPage loginPage = new LoginPage();
        loginPage.open().loginAs(user, pass);
        Assert.assertTrue(loginPage.getFlashMessage().contains(expectedMsg));
    }

    @DataProvider(name = "invalidCredentials", parallel = true)
    public Object[][] invalidCredentials() {
        return new Object[][]{
                {"tomsmith", "wrong", "Your password is invalid!"},
                {"invalid", "SuperSecretPassword!", "Your username is invalid!"},
                {"", "SuperSecretPassword!", "Your username is invalid!"},
                {"tomsmith", "", "Your password is invalid!"},
        };
    }
}
