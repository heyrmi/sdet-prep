package ra.hul.framework.listeners;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import ra.hul.framework.driver.DriverManager;
import ra.hul.framework.pages.BasePage;
import ra.hul.framework.reporting.ReportManager;

/**
 * TestNG Listener — hooks into test lifecycle for:
 * 1. Auto-reporting (pass/fail/skip)
 * 2. Screenshot on failure
 * 3. Centralized logging
 * <p>
 * Registered in testng.xml (not via @Listeners — keeps tests clean)
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        ReportManager.initReports();
        log.info("--- Suite Started: {} ---", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        ReportManager.createTest(testName);
        log.info("--- Test Started: {} ---", testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("--- Test Success: {} ---", testName);
        ReportManager.getTest().log(Status.PASS, "Test Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("--- Test Failed: {} ---", testName);

        // Attach Screenshot for UI tests (driver exists
        try {
            if (DriverManager.getDriver() != null) {
                String base64Screenshot = BasePage.takesScreenshot(testName);
                ReportManager.getTest().fail(result.getThrowable(),
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
            }
        } catch (Exception e) {
            // API won't have a driver
            ReportManager.getTest().fail(result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("--- Test Skipped: {} ---", testName);

        ReportManager.getTest().log(Status.SKIP, "Test Skipped" + result.getThrowable().getMessage());
    }

    @Override
    public void onFinish(ITestContext context) {
        ReportManager.flushReport();
        log.info("--- Suite Finished: {} ---", context.getName());
    }
}
