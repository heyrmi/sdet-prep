package ra.hul.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import ra.hul.framework.constants.FrameworkConstants;

/**
 * Thread-safe Extent Reports manager.
 * <p>
 * ThreadLocal<ExtentTest> ensures parallel tests don't overwrite each other's logs.
 * Synchronized methods prevent race conditions during report initialization.
 */
public class ReportManager {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThreadLocal = new ThreadLocal<>();

    private ReportManager() {
    }

    public static synchronized void initReports() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter(FrameworkConstants.EXTENT_REPORT_PATH);
            spark.config().setTheme(Theme.DARK);
            spark.config().setDocumentTitle("SDET Practice Report");
            spark.config().setReportName("Automation Report");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
            extent.setSystemInfo("Environment", System.getProperty("env", "default"));
        }
    }

    public static synchronized ExtentTest createTest(String testName) {
        ExtentTest test = extent.createTest(testName);
        testThreadLocal.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return testThreadLocal.get();
    }

    /**
     * Remove the current thread's ExtentTest reference.
     * Called after each test completes (pass/fail/skip) to prevent ThreadLocal leaks.
     */
    public static void removeTest() {
        testThreadLocal.remove();
    }

    public static synchronized void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}
