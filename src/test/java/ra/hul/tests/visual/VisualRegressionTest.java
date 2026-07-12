package ra.hul.tests.visual;

import io.qameta.allure.*;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.driver.DriverManager;
import ra.hul.framework.web.utils.VisualRegressionUtils;
import ra.hul.framework.web.utils.VisualRegressionUtils.VisualComparisonResult;
import ra.hul.tests.base.BaseWebTest;

/**
 * Demonstrates the homegrown, offline visual regression capability.
 *
 * <p>On the very first run (no committed baseline yet), the util generates the baseline PNG under
 * {@code src/test/resources/visual/baseline/} and reports {@code baselineCreated=true} — that run
 * is treated as a pass. Subsequent runs pixel-diff the live render against the committed baseline.</p>
 *
 * <p>Runs single-threaded with explicit priority so the baseline is guaranteed to exist before the
 * negative demo compares a deliberately modified page against it.</p>
 */
@Epic("Web Automation")
@Feature("Visual Regression")
public class VisualRegressionTest extends BaseWebTest {

    private static final String BASELINE_NAME = "visual-sample";

    private static WebDriver driver() {
        return DriverManager.getDriver();
    }

    private static String pageUrl(String resource) {
        return VisualRegressionTest.class.getClassLoader().getResource("pages/" + resource).toString();
    }

    @Test(priority = 1, groups = {"regression"},
          description = "Deterministic page matches its committed visual baseline")
    @Severity(SeverityLevel.NORMAL)
    @Story("Baseline match")
    public void visual_deterministicPage_shouldMatchBaseline() {
        driver().get(pageUrl("visual-sample.html"));

        VisualComparisonResult result = VisualRegressionUtils.compare(BASELINE_NAME);
        log.info(result.summary());

        if (result.isBaselineCreated()) {
            // First run in a fresh checkout: baseline was just generated — nothing to compare against.
            Assert.assertTrue(result.isMatch(),
                    "Baseline creation run should pass: " + result.summary());
        } else {
            Assert.assertTrue(result.isMatch(),
                    "Live render drifted from committed baseline beyond threshold: " + result.summary());
        }
    }

    @Test(priority = 2, groups = {"regression"},
          description = "A deliberately modified page is detected as a visual mismatch")
    @Severity(SeverityLevel.MINOR)
    @Story("Negative demo — change detected")
    public void visual_modifiedPage_shouldBeDetectedAsMismatch() {
        // Ensure the baseline exists (generate from the good page if a fresh checkout).
        driver().get(pageUrl("visual-sample.html"));
        VisualComparisonResult ensured = VisualRegressionUtils.compare(BASELINE_NAME);
        log.info("Baseline ensured: {}", ensured.summary());

        // Now render the modified page and compare against the SAME baseline — expect a mismatch.
        driver().get(pageUrl("visual-sample-modified.html"));
        VisualComparisonResult result = VisualRegressionUtils.compare(BASELINE_NAME);
        log.info("Modified comparison: {}", result.summary());

        Assert.assertFalse(result.isMatch(),
                "Modified page should NOT match the baseline: " + result.summary());
        Assert.assertTrue(result.getDiffRatio() > result.getThreshold(),
                "Diff ratio should exceed the configured threshold: " + result.summary());
    }
}
