package ra.hul.framework.web.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

public final class EdgeStrategy implements BrowserStrategy {

    @Override
    public WebDriver createDriver(boolean headless) {
        return new EdgeDriver((EdgeOptions) getCapabilities(headless));
    }

    @Override
    public MutableCapabilities getCapabilities(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080"
        );
        return options;
    }
}
