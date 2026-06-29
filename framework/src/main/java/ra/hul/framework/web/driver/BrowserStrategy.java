package ra.hul.framework.web.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

/**
 * Sealed interface for browser creation strategy (Java 21 feature).
 * Only the declared implementations can exist — no accidental extension.
 */
public sealed interface BrowserStrategy
        permits ChromeStrategy, FirefoxStrategy, EdgeStrategy {

    WebDriver createDriver(boolean headless);

    MutableCapabilities getCapabilities(boolean headless);
}
