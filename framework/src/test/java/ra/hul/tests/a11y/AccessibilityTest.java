package ra.hul.tests.a11y;

import com.deque.html.axecore.results.Rule;
import io.qameta.allure.*;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.driver.DriverManager;
import ra.hul.framework.web.utils.AccessibilityUtils;
import ra.hul.tests.base.BaseWebTest;

import java.util.List;

/**
 * Demonstrates the axe-core accessibility capability against a bundled, fully-offline page that
 * contains deterministic, known WCAG violations (missing image alt, unlabelled input, low contrast)
 * plus a clean subtree that should pass.
 */
@Epic("Web Automation")
@Feature("Accessibility (axe-core)")
public class AccessibilityTest extends BaseWebTest {

    private static WebDriver driver() {
        return DriverManager.getDriver();
    }

    private static String pageUrl() {
        return AccessibilityTest.class.getClassLoader().getResource("pages/a11y-sample.html").toString();
    }

    @Test(groups = {"regression"},
          description = "axe-core detects the known WCAG violations on the sample page")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Known violations detected")
    public void a11y_samplePage_shouldDetectKnownViolations() {
        driver().get(pageUrl());

        List<Rule> violations = AccessibilityUtils.analyze();

        Assert.assertFalse(violations.isEmpty(), "Expected the sample page to have violations");
        Assert.assertTrue(AccessibilityUtils.containsRule(violations, "image-alt"),
                "Expected an 'image-alt' violation (img without alt). Found: " + ruleIds(violations));
        Assert.assertTrue(AccessibilityUtils.containsRule(violations, "label"),
                "Expected a 'label' violation (input without label). Found: " + ruleIds(violations));
        Assert.assertTrue(AccessibilityUtils.containsRule(violations, "color-contrast"),
                "Expected a 'color-contrast' violation (low-contrast text). Found: " + ruleIds(violations));
    }

    @Test(groups = {"regression"},
          description = "The clean subtree passes the accessibility scan")
    @Severity(SeverityLevel.NORMAL)
    @Story("Clean subtree passes")
    public void a11y_cleanSubtree_shouldHaveNoViolations() {
        driver().get(pageUrl());

        List<Rule> violations = AccessibilityUtils.analyzeSelector("#clean");

        Assert.assertTrue(violations.isEmpty(),
                "Clean subtree should have no violations but found: " + ruleIds(violations));
    }

    private static String ruleIds(List<Rule> violations) {
        return violations.stream().map(Rule::getId).toList().toString();
    }
}
