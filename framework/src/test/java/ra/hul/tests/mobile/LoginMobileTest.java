package ra.hul.tests.mobile;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.mobile.screens.HomeScreen;
import ra.hul.framework.mobile.screens.LoginScreen;
import ra.hul.tests.base.BaseMobileTest;

@Epic("Mobile Automation")
@Feature("Mobile Login")
public class LoginMobileTest extends BaseMobileTest {

    @Test(groups = {"smoke", "regression"},
          description = "Login with valid credentials on mobile app")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Mobile Login Flow")
    public void mobileLogin_validCredentials_shouldShowSuccess() {
        HomeScreen home = new HomeScreen();
        LoginScreen loginScreen = home.navigateToLogin();

        loginScreen.selectLoginTab()
                .enterEmail("test@test.com")
                .enterPassword("Password123")
                .clickLogin();

        Assert.assertTrue(loginScreen.isSuccessDialogDisplayed(),
                "Success dialog should be displayed after valid login");
    }

    @Test(groups = {"regression"},
          description = "Login and dismiss success dialog")
    @Severity(SeverityLevel.NORMAL)
    @Story("Mobile Login Flow")
    public void mobileLogin_dismissDialog_shouldReturnToLoginScreen() {
        HomeScreen home = new HomeScreen();
        LoginScreen loginScreen = home.navigateToLogin();

        loginScreen.selectLoginTab()
                .loginAs("test@test.com", "Password123")
                .dismissSuccessDialog();

        Assert.assertTrue(loginScreen.isLoaded(),
                "Should return to login screen after dismissing dialog");
    }
}
