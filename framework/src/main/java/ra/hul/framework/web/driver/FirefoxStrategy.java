package ra.hul.framework.web.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public final class FirefoxStrategy implements BrowserStrategy {

    @Override
    public WebDriver createDriver(boolean headless) {
        return new FirefoxDriver((FirefoxOptions) getCapabilities(headless));
    }

    @Override
    public MutableCapabilities getCapabilities(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("--headless");
        }
        options.addArguments("--width=1920", "--height=1080");
        return options;
    }
}
