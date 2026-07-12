package ra.hul.framework.mobile.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.core.constants.FrameworkConstants;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

/**
 * Factory for creating Appium driver instances.
 * Supports both Android (UiAutomator2) and iOS (XCUITest).
 */
public final class AppiumDriverFactory {

    private AppiumDriverFactory() {
    }

    public static AppiumDriver createDriver(String platform) {
        String serverUrl = ConfigManager.get("appium.server.url");

        return switch (platform.toLowerCase()) {
            case FrameworkConstants.ANDROID -> createAndroidDriver(serverUrl);
            case FrameworkConstants.IOS -> createIOSDriver(serverUrl);
            default -> throw new IllegalArgumentException("Unsupported mobile platform: " + platform);
        };
    }

    private static AppiumDriver createAndroidDriver(String serverUrl) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(ConfigManager.getOrDefault("android.device.name", "emulator-5554"))
                .setApp(ConfigManager.get("android.app.path"))
                .setAutomationName("UiAutomator2")
                .setNoReset(ConfigManager.getBoolean("appium.no.reset"))
                .setNewCommandTimeout(Duration.ofSeconds(
                        ConfigManager.getIntOrDefault("appium.command.timeout", 60)));

        String appPackage = ConfigManager.getOrDefault("android.app.package", null);
        if (appPackage != null) {
            options.setAppPackage(appPackage);
            options.setAppActivity(ConfigManager.get("android.app.activity"));
        }

        try {
            return new AndroidDriver(URI.create(serverUrl).toURL(), options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium server URL: " + serverUrl, e);
        }
    }

    private static AppiumDriver createIOSDriver(String serverUrl) {
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(ConfigManager.getOrDefault("ios.device.name", "iPhone 15"))
                .setPlatformVersion(ConfigManager.getOrDefault("ios.platform.version", "17.0"))
                .setApp(ConfigManager.get("ios.app.path"))
                .setAutomationName("XCUITest")
                .setNoReset(ConfigManager.getBoolean("appium.no.reset"));

        try {
            return new IOSDriver(URI.create(serverUrl).toURL(), options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium server URL: " + serverUrl, e);
        }
    }
}
