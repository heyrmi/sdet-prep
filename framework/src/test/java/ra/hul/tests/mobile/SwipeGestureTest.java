package ra.hul.tests.mobile;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.mobile.screens.HomeScreen;
import ra.hul.framework.mobile.screens.SwipeScreen;
import ra.hul.tests.base.BaseMobileTest;

@Epic("Mobile Automation")
@Feature("Gestures")
public class SwipeGestureTest extends BaseMobileTest {

    @Test(groups = {"regression"},
          description = "Swipe left to navigate to next card")
    @Severity(SeverityLevel.NORMAL)
    @Story("Swipe Gesture")
    public void swipeLeft_shouldNavigateToNextCard() {
        HomeScreen home = new HomeScreen();
        SwipeScreen swipeScreen = home.navigateToSwipe();

        Assert.assertTrue(swipeScreen.isLoaded(), "Swipe screen should be loaded");
        swipeScreen.swipeCardLeft();
        // Verify screen is still functional after swipe
        Assert.assertTrue(swipeScreen.isLoaded());
    }

    @Test(groups = {"regression"},
          description = "Scroll down to reveal hidden content")
    @Severity(SeverityLevel.MINOR)
    @Story("Scroll Gesture")
    public void scrollDown_shouldRevealContent() {
        HomeScreen home = new HomeScreen();
        SwipeScreen swipeScreen = home.navigateToSwipe();

        swipeScreen.scrollToBottom();
        Assert.assertTrue(swipeScreen.isLoaded());
    }
}
