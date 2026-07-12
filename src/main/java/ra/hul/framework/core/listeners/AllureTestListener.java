package ra.hul.framework.core.listeners;

import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that captures screenshots on test failure and attaches them to Allure reports.
 * Retrieves the driver from ITestResult attributes set by base test classes.
 */
public class AllureTestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(AllureTestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        log.info("Starting: {}.{}", result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("Passed: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("Failed: {} - {}", result.getMethod().getMethodName(), result.getThrowable().getMessage());
        Object driver = result.getAttribute("driver");
        if (driver instanceof WebDriver webDriver) {
            attachScreenshot(webDriver);
        } else {
            log.debug("No driver available for screenshot (likely an API test)");
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("Skipped: {}", result.getMethod().getMethodName());
    }

    @Attachment(value = "Failure Screenshot", type = "image/png")
    private byte[] attachScreenshot(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
