package ra.hul.tests.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import ra.hul.framework.driver.DriverManager;

/**
 * Base class for all UI tests.
 * <p>
 * Lifecycle:
 *
 * @BeforeMethod → init driver (each test gets fresh browser)
 * @AfterMethod → quit driver (cleanup guaranteed even on failure)
 * <p>
 * Why @BeforeMethod and not @BeforeClass?
 * - Test isolation: each test gets a clean browser state
 * - Parallel safety: no shared mutable state between tests
 * - Failure containment: one test's crash doesn't affect others
 */
public class BaseTest {

    protected final Logger log = LogManager.getLogger(BaseTest.class);

    @BeforeMethod
    public void setUp() {
        DriverManager.initDriver();
    }

    // alwaysRun = true (Ensure cleanup even on test failure)
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
