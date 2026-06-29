package ra.hul.tests.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import ra.hul.framework.mobile.driver.AppiumDriverManager;

public class BaseMobileTest {

    protected final Logger log = LogManager.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        AppiumDriverManager.initDriver();
        result.setAttribute("driver", AppiumDriverManager.getDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        AppiumDriverManager.quitDriver();
    }
}
