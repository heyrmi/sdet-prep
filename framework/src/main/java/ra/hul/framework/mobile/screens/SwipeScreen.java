package ra.hul.framework.mobile.screens;

import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class SwipeScreen extends BaseScreen {

    private final By swipeScreenTitle = AppiumBy.accessibilityId("Swipe-screen");
    private final By card = AppiumBy.accessibilityId("card");

    @Step("Get current card title")
    public String getCurrentCardTitle() {
        return getText(card);
    }

    @Step("Swipe left on card")
    public SwipeScreen swipeCardLeft() {
        swipeLeft();
        return this;
    }

    @Step("Swipe right on card")
    public SwipeScreen swipeCardRight() {
        swipeRight();
        return this;
    }

    @Step("Scroll down to find hidden content")
    public SwipeScreen scrollToBottom() {
        scrollDown();
        return this;
    }

    @Override
    public boolean isLoaded() {
        return waitUntilDisplayed(swipeScreenTitle);
    }
}
