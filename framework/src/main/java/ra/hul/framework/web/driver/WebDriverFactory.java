package ra.hul.framework.web.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.core.constants.FrameworkConstants;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

/**
 * Factory for creating WebDriver instances.
 * Supports local browsers and RemoteWebDriver (Selenium Grid / cloud providers).
 */
public final class WebDriverFactory {

    private static final Map<String, BrowserStrategy> STRATEGIES = Map.of(
            FrameworkConstants.CHROME, new ChromeStrategy(),
            FrameworkConstants.FIREFOX, new FirefoxStrategy(),
            FrameworkConstants.EDGE, new EdgeStrategy()
    );

    private WebDriverFactory() {
    }

    public static WebDriver createDriver(String browser, boolean headless) {
        BrowserStrategy strategy = STRATEGIES.get(browser.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported browser: " + browser + ". Supported: " + STRATEGIES.keySet());
        }

        String gridUrl = ConfigManager.getOrDefault("grid.url", null);
        if (gridUrl != null && !gridUrl.isBlank()) {
            return createRemoteDriver(gridUrl, strategy, headless);
        }

        return strategy.createDriver(headless);
    }

    private static WebDriver createRemoteDriver(String gridUrl, BrowserStrategy strategy, boolean headless) {
        try {
            return new RemoteWebDriver(
                    URI.create(gridUrl).toURL(),
                    strategy.getCapabilities(headless)
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid grid URL: " + gridUrl, e);
        }
    }
}
