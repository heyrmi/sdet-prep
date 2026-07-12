package ra.hul.sdet.designpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator Pattern for Enhanced WebDriver - wrap a Driver to add cross-cutting behavior, stackable.
 * Common SDET question: "Add logging/screenshots around every WebDriver action without changing it."
 *
 * Self-contained: {@code Driver} is a tiny stub instead of org.openqa.selenium.WebDriver.
 * Open/Closed: the core StubDriver is never modified; new behavior is added by wrapping.
 * Decorators share the interface, so they stack (logging around screenshots around the real driver).
 * Run main() — no browser needed.
 */
public class Ques7_DecoratorWebDriver {

    interface Driver {
        void get(String url);
        void click(String locator);
        String title();
    }

    /** The concrete component being decorated (real: ChromeDriver). */
    static final class StubDriver implements Driver {
        private String url = "about:blank";
        public void get(String u) { this.url = u; }
        public void click(String locator) { /* real: findElement(locator).click() */ }
        public String title() { return "Stub[" + url + "]"; }
    }

    /** Base decorator: implements Driver by delegating to the wrapped Driver. */
    static abstract class DriverDecorator implements Driver {
        protected final Driver delegate;
        DriverDecorator(Driver delegate) { this.delegate = delegate; }
        public void get(String url) { delegate.get(url); }
        public void click(String locator) { delegate.click(locator); }
        public String title() { return delegate.title(); }
    }

    /** Logs before/after each action, then delegates. */
    static final class LoggingDriver extends DriverDecorator {
        private final List<String> log;
        LoggingDriver(Driver delegate, List<String> log) { super(delegate); this.log = log; }
        public void get(String url) {
            log.add("LOG before get(" + url + ")");
            super.get(url);
            log.add("LOG after get(" + url + ")");
        }
        public void click(String locator) {
            log.add("LOG before click(" + locator + ")");
            super.click(locator);
            log.add("LOG after click(" + locator + ")");
        }
    }

    /** Captures a "screenshot" (stub) right after each click, then delegates. */
    static final class ScreenshotDriver extends DriverDecorator {
        private final List<String> log;
        ScreenshotDriver(Driver delegate, List<String> log) { super(delegate); this.log = log; }
        public void click(String locator) {
            super.click(locator);
            log.add("SHOT screenshot after click(" + locator + ")"); // real: getScreenshotAs(OutputType.FILE)
        }
    }

    static void main() {
        List<String> log = new ArrayList<>();

        // Stack decorators: Logging( Screenshot( StubDriver ) ).
        Driver driver = new LoggingDriver(new ScreenshotDriver(new StubDriver(), log), log);

        driver.get("https://example.com");
        driver.click("id=submit");
        System.out.println("Title: " + driver.title());
        System.out.println("Action trace:");
        log.forEach(l -> System.out.println("  " + l));

        boolean ok = log.contains("LOG before click(id=submit)")
                && log.contains("SHOT screenshot after click(id=submit)")
                && log.contains("LOG after click(id=submit)");
        System.out.println(ok ? "PASSED: both decorators wrapped the driver and fired around click."
                              : "FAILED: decorator stacking unexpected.");
    }
}
