package ra.hul.tests.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import ra.hul.framework.web.driver.DriverManager;

public class BaseWebTest {

    protected final Logger log = LogManager.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        DriverManager.initDriver();
        result.setAttribute("driver", DriverManager.getDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
