package ra.hul.sdet.designpatterns;

/**
 * Factory Pattern for WebDriver creation - build a Driver from a browser type, hide construction details.
 * Common SDET question: "Create a WebDriverFactory that returns Chrome/Firefox/Edge/Remote by config."
 *
 * Self-contained: {@code Driver} is a tiny stub instead of org.openqa.selenium.WebDriver.
 * Real usage: each branch would new ChromeDriver(chromeOptions) / new RemoteWebDriver(gridUrl, caps) etc.
 * Open/Closed: add a new Browser enum + one switch arm to support a new browser; callers never change.
 * Run main() — no browser needed.
 */
public class Ques3_FactoryWebDriver {

    interface Driver { String name(); boolean headless(); }

    enum Browser { CHROME, FIREFOX, EDGE, SAFARI, REMOTE }

    /** Small options bag (stands in for ChromeOptions/FirefoxOptions + Selenium Grid URL). */
    record Options(boolean headless, String gridUrl) {
        static Options defaults() { return new Options(false, null); }
    }

    static final class WebDriverFactory {
        private WebDriverFactory() {}
        static Driver create(Browser browser, Options opts) {
            // Real: configure browser-specific options, then instantiate the concrete driver.
            return switch (browser) {
                case CHROME  -> stub("ChromeDriver", opts.headless());
                case FIREFOX -> stub("FirefoxDriver", opts.headless());
                case EDGE    -> stub("EdgeDriver", opts.headless());
                case SAFARI  -> stub("SafariDriver", false); // Safari has no headless mode
                case REMOTE  -> {
                    if (opts.gridUrl() == null) throw new IllegalArgumentException("REMOTE needs a grid URL");
                    yield stub("RemoteWebDriver@" + opts.gridUrl(), opts.headless());
                }
            };
        }
        private static Driver stub(String label, boolean headless) {
            return new Driver() {
                public String name() { return label; }
                public boolean headless() { return headless; }
            };
        }
    }

    static void main() {
        Driver chrome = WebDriverFactory.create(Browser.CHROME, new Options(true, null));
        Driver firefox = WebDriverFactory.create(Browser.FIREFOX, Options.defaults());
        Driver remote = WebDriverFactory.create(Browser.REMOTE, new Options(true, "http://grid:4444"));

        System.out.println("Created: " + chrome.name() + " (headless=" + chrome.headless() + ")");
        System.out.println("Created: " + firefox.name() + " (headless=" + firefox.headless() + ")");
        System.out.println("Created: " + remote.name() + " (headless=" + remote.headless() + ")");

        boolean guardOk;
        try { WebDriverFactory.create(Browser.REMOTE, Options.defaults()); guardOk = false; }
        catch (IllegalArgumentException e) { guardOk = true; }

        boolean ok = chrome.name().equals("ChromeDriver") && chrome.headless()
                && remote.name().contains("grid:4444") && guardOk;
        System.out.println(ok ? "PASSED: factory produced the right drivers and validated inputs."
                              : "FAILED: factory output unexpected.");
    }
}
