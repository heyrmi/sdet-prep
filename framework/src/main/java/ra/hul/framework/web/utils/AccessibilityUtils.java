package ra.hul.framework.web.utils;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.deque.html.axecore.selenium.AxeReporter;
import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.web.driver.DriverManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin wrapper around the axe-core Selenium binding ({@link AxeBuilder}) that runs a WCAG
 * accessibility scan of the current page (or a subtree) entirely offline — axe-core is injected
 * into the page by the binding, no network calls.
 *
 * <p>Config (read via {@code getOrDefault} so absence never crashes):</p>
 * <ul>
 *   <li>{@code a11y.tags} — comma-separated axe tag filter (default {@code wcag2a,wcag2aa})</li>
 * </ul>
 *
 * <p>Violations (both raw JSON and a human-readable summary) are attached to the Allure report.
 * This class never asserts — it returns the violations and the test decides pass/fail.</p>
 */
public final class AccessibilityUtils {

    private static final Logger log = LogManager.getLogger(AccessibilityUtils.class);
    private static final String DEFAULT_TAGS = "wcag2a,wcag2aa";

    private AccessibilityUtils() {
    }

    /** Scan the whole current page with the configured WCAG tag filter. */
    public static List<Rule> analyze() {
        return analyze(configuredTags());
    }

    /** Scan the whole current page with an explicit tag list. */
    public static List<Rule> analyze(List<String> tags) {
        Results results = runScan(new AxeBuilder(), tags);
        return report(results, "page");
    }

    /**
     * Scan only the subtree matched by a CSS selector (used to demonstrate that a clean
     * region passes even when the wider page has violations).
     */
    public static List<Rule> analyzeSelector(String cssSelector) {
        return analyzeSelector(cssSelector, configuredTags());
    }

    public static List<Rule> analyzeSelector(String cssSelector, List<String> tags) {
        Results results = runScan(new AxeBuilder().include(List.of(cssSelector)), tags);
        return report(results, "selector '" + cssSelector + "'");
    }

    // ---------------------------------------------------------------------------------------------

    private static Results runScan(AxeBuilder builder, List<String> tags) {
        WebDriver driver = DriverManager.getDriver();
        Results results = builder.withTags(tags).analyze(driver);
        if (results.isErrored()) {
            throw new IllegalStateException("axe-core scan errored: " + results.getErrorMessage());
        }
        return results;
    }

    private static List<Rule> report(Results results, String scope) {
        List<Rule> violations = results.getViolations();
        log.info("Accessibility scan of {} found {} violation rule(s)", scope, violations.size());
        attachViolationsJson(AxeReporter.serialize(violations));
        attachViolationsSummary(buildSummary(violations, scope));
        return violations;
    }

    private static List<String> configuredTags() {
        String raw = ConfigManager.getOrDefault("a11y.tags", DEFAULT_TAGS);
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** Build a readable, deterministic summary of the violations for the Allure report / logs. */
    public static String buildSummary(List<Rule> violations, String scope) {
        if (violations.isEmpty()) {
            return "No accessibility violations found for " + scope + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(violations.size()).append(" accessibility violation rule(s) for ").append(scope).append(":\n");
        for (Rule rule : violations) {
            List<CheckedNode> nodes = rule.getNodes();
            sb.append("  • [").append(rule.getImpact()).append("] ")
                    .append(rule.getId()).append(" — ").append(rule.getHelp())
                    .append(" (").append(nodes == null ? 0 : nodes.size()).append(" node(s))\n");
            if (nodes != null) {
                for (CheckedNode node : nodes) {
                    sb.append("      target=").append(node.getTarget())
                            .append(" html=").append(compact(node.getHtml())).append('\n');
                }
            }
        }
        return sb.toString();
    }

    /** Convenience: true if any returned violation matches the given rule id. */
    public static boolean containsRule(List<Rule> violations, String ruleId) {
        return violations.stream().anyMatch(r -> ruleId.equals(r.getId()));
    }

    private static String compact(String html) {
        if (html == null) {
            return "";
        }
        String collapsed = html.replaceAll("\\s+", " ").trim();
        return collapsed.length() > 120 ? collapsed.substring(0, 117) + "..." : collapsed;
    }

    @Attachment(value = "Accessibility Violations (JSON)", type = "application/json")
    private static String attachViolationsJson(String json) {
        return json;
    }

    @Attachment(value = "Accessibility Violations (Summary)", type = "text/plain")
    private static String attachViolationsSummary(String summary) {
        return summary;
    }
}
