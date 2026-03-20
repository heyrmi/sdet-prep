package ra.hul.sdet.selenium;

import io.restassured.RestAssured;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

/**
 * Broken Link Checker - Use Selenium to find all links on a page, then REST Assured to validate each.
 * Common SDET question: "Check all links on a webpage and report which ones are broken."
 *
 * Target: https://the-internet.herokuapp.com (safe test site)
 */
public class Ques1_BrokenLinkChecker {

    static void main() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);
        int valid = 0, broken = 0, skipped = 0;

        try {
            driver.get("https://the-internet.herokuapp.com");
            List<WebElement> links = driver.findElements(By.tagName("a"));
            System.out.println("Found " + links.size() + " links. Checking...\n");

            for (WebElement link : links) {
                String href = link.getAttribute("href");
                if (href == null || href.isEmpty() || href.startsWith("mailto:") || href.startsWith("javascript:")) {
                    skipped++;
                    continue;
                }

                try {
                    int statusCode = RestAssured
                            .given().relaxedHTTPSValidation().redirects().follow(false)
                            .head(href)
                            .getStatusCode();

                    if (statusCode >= 400) {
                        System.out.printf("BROKEN [%d]: %s%n", statusCode, href);
                        broken++;
                    } else {
                        valid++;
                    }
                } catch (Exception e) {
                    System.out.printf("ERROR: %s -> %s%n", href, e.getMessage());
                    broken++;
                }
            }

            System.out.printf("%nSummary: %d valid | %d broken | %d skipped%n", valid, broken, skipped);
        } finally {
            driver.quit();
        }
    }
}
