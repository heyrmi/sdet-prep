package ra.hul.sdet.designpatterns;

/**
 * Page Object Model (POM) - One class per page, elements hidden behind action methods, fluent navigation.
 * Common SDET question: "Design a Page Object Model with a fluent API returning the next page object."
 *
 * Self-contained: uses a tiny local {@code Driver} stub instead of the real org.openqa.selenium.WebDriver.
 * In real Selenium, page elements would be {@code @FindBy WebElement} fields wired by
 * {@code PageFactory.initElements(driver, this)}, and actions would call {@code element.click()} / {@code sendKeys()}.
 * Run main() — no browser needed.
 */
public class Ques1_PageObjectModel {

    /** Minimal stand-in for org.openqa.selenium.WebDriver. */
    interface Driver {
        void get(String url);
        void type(String locator, String text);
        void click(String locator);
        String title();
        String currentUrl();
    }

    /** No-op stub: records navigation so the demo can print/verify without a real browser. */
    static class StubDriver implements Driver {
        private String url = "about:blank";
        public void get(String u) { this.url = u; }
        public void type(String locator, String text) { /* real: findElement(locator).sendKeys(text) */ }
        public void click(String locator) { this.url = url + "#" + locator; /* real: findElement(locator).click() */ }
        public String title() { return "Stub[" + url + "]"; }
        public String currentUrl() { return url; }
    }

    /** Common base page: holds the driver and shared helpers (waits, title, etc.). */
    static abstract class BasePage {
        protected final Driver driver;
        BasePage(Driver driver) { this.driver = driver; }
        String pageTitle() { return driver.title(); }
    }

    static class LoginPage extends BasePage {
        // Real Selenium: @FindBy(id="user") WebElement username; @FindBy(id="pass") WebElement password;
        private static final String USERNAME = "id=user", PASSWORD = "id=pass", SUBMIT = "id=login";
        LoginPage(Driver driver) { super(driver); driver.get("https://shop.example/login"); }

        /** Fluent action: fills credentials and returns the NEXT page object. */
        SearchPage loginAs(String user, String pass) {
            driver.type(USERNAME, user);
            driver.type(PASSWORD, pass);
            driver.click(SUBMIT);
            System.out.println("  LoginPage: submitted credentials for '" + user + "'");
            return new SearchPage(driver);
        }
    }

    static class SearchPage extends BasePage {
        private static final String BOX = "id=q", GO = "id=go";
        SearchPage(Driver driver) { super(driver); }

        CartPage searchAndAddFirst(String term) {
            driver.type(BOX, term);
            driver.click(GO);
            driver.click("css=.product:first-child .add-to-cart");
            System.out.println("  SearchPage: searched '" + term + "' and added first result to cart");
            return new CartPage(driver);
        }
    }

    static class CartPage extends BasePage {
        CartPage(Driver driver) { super(driver); }
        boolean hasItems() { return driver.currentUrl().contains("add-to-cart"); }
    }

    static void main() {
        Driver driver = new StubDriver();

        // Fluent chain: each page action hands back the next page object.
        CartPage cart = new LoginPage(driver)
                .loginAs("rahul", "secret")
                .searchAndAddFirst("wireless mouse");

        boolean ok = cart.hasItems();
        System.out.println("Final page title: " + cart.pageTitle());
        System.out.println(ok ? "PASSED: POM fluent flow reached a non-empty cart." : "FAILED: cart is empty.");
    }
}
