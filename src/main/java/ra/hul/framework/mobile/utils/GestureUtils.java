package ra.hul.framework.mobile.utils;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;

/**
 * Mobile gesture utilities using W3C Actions API (Appium 2.x compatible).
 */
public final class GestureUtils {

    private GestureUtils() {
    }

    public static void swipeLeft(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        int startX = (int) (size.width * 0.8);
        int endX = (int) (size.width * 0.2);
        int y = size.height / 2;
        swipe(driver, startX, y, endX, y);
    }

    public static void swipeRight(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        int startX = (int) (size.width * 0.2);
        int endX = (int) (size.width * 0.8);
        int y = size.height / 2;
        swipe(driver, startX, y, endX, y);
    }

    public static void scrollDown(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        int x = size.width / 2;
        int startY = (int) (size.height * 0.7);
        int endY = (int) (size.height * 0.3);
        swipe(driver, x, startY, x, endY);
    }

    public static void scrollUp(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        int x = size.width / 2;
        int startY = (int) (size.height * 0.3);
        int endY = (int) (size.height * 0.7);
        swipe(driver, x, startY, x, endY);
    }

    private static void swipe(AppiumDriver driver, int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(
                Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(
                Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(swipe));
    }
}
