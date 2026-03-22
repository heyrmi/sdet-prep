package ra.hul.tests.ui;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import ra.hul.framework.pages.LoginPage;
import ra.hul.framework.pages.SecurePage;
import ra.hul.tests.base.BaseTest;

/**
 * UI tests for the-internet.herokuapp.com/login
 * Valid credentials: tomsmith / SuperSecretPassword!
 */
public class LoginTest extends BaseTest {

    @Test(description = "Verify successful login with valid credentials")
    public void login_validCredentials_shouldShowSecurePage() {
        LoginPage loginPage = new LoginPage();
        loginPage.open()
                .enterUsername("tomsmith")
                .enterPassword("SuperSecretPassword!");
        loginPage.clickLogin();

        SecurePage securePage = new SecurePage();
        Assert.assertTrue(securePage.getFlashMessage().contains("You logged into a secure area!"),
                "Success flash message should be displayed");
        Assert.assertTrue(securePage.isLogoutButtonDisplayed(),
                "Logout button should be visible after login");
    }

    @Test(description = "Verify error message on invalid credentials")
    public void login_invalidPassword_shouldShowError() {
        LoginPage loginPage = new LoginPage();
        loginPage.open()
                .loginAs("tomsmith", "WrongPassword");

        Assert.assertTrue(loginPage.getFlashMessage().contains("Your password is invalid!"),
                "Error flash message should mention invalid password");
    }

    @Test(description = "Verify error on invalid username")
    public void login_invalidUsername_shouldShowError() {
        LoginPage loginPage = new LoginPage();
        loginPage.open()
                .loginAs("invaliduser", "SuperSecretPassword!");

        Assert.assertTrue(loginPage.getFlashMessage().contains("Your username is invalid!"),
                "Error flash message should mention invalid username");
    }

    @Test(description = "Verify login and then logout flow")
    public void login_thenLogout_shouldReturnToLoginPage() {
        LoginPage loginPage = new LoginPage();
        loginPage.open()
                .loginAs("tomsmith", "SuperSecretPassword!");

        SecurePage securePage = new SecurePage();
        LoginPage returnedLoginPage = securePage.clickLogout();

        Assert.assertTrue(returnedLoginPage.getFlashMessage().contains("You logged out of the secure area!"),
                "Logout success message should be displayed");
    }

    @Test(description = "Verify multiple fields using SoftAssert",
            dataProvider = "invalidCredentials")
    public void login_invalidData_shouldShowAppropriateError(
            String username, String password, String expectedError) {
        LoginPage loginPage = new LoginPage();
        loginPage.open()
                .loginAs(username, password);

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(loginPage.getFlashMessage().contains(expectedError),
                "Expected error containing: " + expectedError);
        soft.assertAll();
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][]{
                {"tomsmith", "wrong", "Your password is invalid!"},
                {"invaliduser", "SuperSecretPassword!", "Your username is invalid!"},
                {"", "SuperSecretPassword!", "Your username is invalid!"},
                {"tomsmith", "", "Your password is invalid!"},
        };
    }
}