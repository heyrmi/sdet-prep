package ra.hul.framework.web.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public final class ChromeStrategy implements BrowserStrategy {

    @Override
    public WebDriver createDriver(boolean headless) {
        return new ChromeDriver((ChromeOptions) getCapabilities(headless));
    }

    @Override
    public MutableCapabilities getCapabilities(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--disable-extensions",
                "--disable-infobars"
        );
        return options;
    }
}
