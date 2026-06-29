package ra.hul.tests.mobile;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.mobile.screens.HomeScreen;
import ra.hul.tests.base.BaseMobileTest;

@Epic("Mobile Automation")
@Feature("Navigation")
public class NavigationTest extends BaseMobileTest {

    @Test(groups = {"smoke", "regression"},
          description = "Navigate between tabs in the app")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Tab Navigation")
    public void navigation_switchTabs_shouldLoadCorrectScreens() {
        HomeScreen home = new HomeScreen();
        Assert.assertTrue(home.isLoaded(), "Home screen should be loaded on app launch");

        // Navigate to Login
        var loginScreen = home.navigateToLogin();
        Assert.assertTrue(loginScreen.isLoaded(), "Login screen should be loaded");

        // Navigate to Forms (back via home)
        home = new HomeScreen();
        var formScreen = home.navigateToForms();
        Assert.assertTrue(formScreen.isLoaded(), "Form screen should be loaded");
    }

    @Test(groups = {"regression"},
          description = "Navigate to Swipe screen and back")
    @Severity(SeverityLevel.NORMAL)
    @Story("Tab Navigation")
    public void navigation_toSwipeAndBack_shouldWork() {
        HomeScreen home = new HomeScreen();
        var swipeScreen = home.navigateToSwipe();
        Assert.assertTrue(swipeScreen.isLoaded(), "Swipe screen should be loaded");

        home.navigateToHome();
        Assert.assertTrue(home.isLoaded(), "Home screen should be loaded after navigating back");
    }
}
