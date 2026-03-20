package ra.hul.sdet.scraping;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

/**
 * Scrape Page Titles - Visit a website and extract all link texts from the page.
 * Common SDET question: "Open a webpage and scrape all the anchor tag texts."
 *
 * Target: https://news.ycombinator.com (Hacker News - simple, no auth needed)
 */
public class Ques1_ScrapePageTitles {

    public static void main(String[] args) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get("https://news.ycombinator.com");
            System.out.println("Page Title: " + driver.getTitle());
            System.out.println("---");

            List<WebElement> storyLinks = driver.findElements(By.cssSelector(".titleline > a"));
            System.out.println("Found " + storyLinks.size() + " stories:\n");

            int rank = 1;
            for (WebElement link : storyLinks) {
                String title = link.getText();
                String href = link.getAttribute("href");
                System.out.printf("%2d. %s%n    %s%n%n", rank++, title, href);
            }
        } finally {
            driver.quit();
        }
    }
}
