package ra.hul.sdet.aiqa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Golden Dataset Runner - the eval harness that turns "does it feel better?" into a number.
 *
 * <p>The golden dataset is the durable asset of an AI system. Models change, prompts get rewritten,
 * the retrieval stack gets replaced — the dataset outlives all of it. Two rules make it useful:
 *
 * <ol>
 *   <li><b>Build it from real production failures</b>, not synthetic examples. Synthetic cases test
 *       what you already thought of, which is exactly the set of things that are not broken.</li>
 *   <li><b>Version everything the score depends on</b> — prompt, model ID, parameters, retrieval
 *       corpus, scorer code. A score without provenance is not reproducible, and an irreproducible
 *       score cannot gate a merge.</li>
 * </ol>
 *
 * <p>This runner executes a suite of cases against a system-under-test, scores each with
 * deterministic assertions, and produces a report broken down by tag — because "82% overall" hides
 * that the high-stakes cases are at 40%.
 *
 * <p>Interview angle: "how would you know a prompt change made things better?" — this, plus the
 * regression gate in {@link Ques6_EvalRegressionGate}.
 *
 * <p>Self-contained: the system-under-test is a stubbed function, so the harness is exercised
 * without any model call.
 */
public class Ques2_GoldenDatasetRunner {

    /** One evaluation case. Assertions where possible; a rubric only for the subjective remainder. */
    record GoldenCase(
            String id,
            String input,
            List<String> mustContain,
            List<String> mustNotContain,
            List<String> tags,
            String source) {
    }

    /** The outcome of running one case. */
    record CaseResult(GoldenCase testCase, String output, boolean passed, String failureReason) {
    }

    /** Aggregate report over a suite run. */
    record EvalReport(
            List<CaseResult> results,
            double passRate,
            Map<String, Double> passRateByTag) {

        int passedCount() {
            return (int) results.stream().filter(CaseResult::passed).count();
        }

        List<CaseResult> failures() {
            return results.stream().filter(r -> !r.passed()).toList();
        }
    }

    /**
     * Runs every case against the system under test.
     *
     * @param systemUnderTest maps an input prompt to the system's output
     */
    static EvalReport run(List<GoldenCase> suite, Function<String, String> systemUnderTest) {
        List<CaseResult> results = new ArrayList<>();

        for (GoldenCase c : suite) {
            String output = systemUnderTest.apply(c.input());
            String lower = output.toLowerCase(Locale.ROOT);

            List<String> missing = new ArrayList<>();
            for (String phrase : c.mustContain()) {
                if (!lower.contains(phrase.toLowerCase(Locale.ROOT))) {
                    missing.add(phrase);
                }
            }
            List<String> forbidden = new ArrayList<>();
            for (String phrase : c.mustNotContain()) {
                if (lower.contains(phrase.toLowerCase(Locale.ROOT))) {
                    forbidden.add(phrase);
                }
            }

            boolean passed = missing.isEmpty() && forbidden.isEmpty();
            String reason = passed ? ""
                    : (missing.isEmpty() ? "" : "missing " + missing)
                            + (forbidden.isEmpty() ? "" : (missing.isEmpty() ? "" : "; ") + "forbidden " + forbidden);
            results.add(new CaseResult(c, output, passed, reason));
        }

        double passRate = results.isEmpty() ? 0
                : (double) results.stream().filter(CaseResult::passed).count() / results.size();

        // Per-tag breakdown. An aggregate number hides the distribution, and the distribution is
        // where the decision lives: 82% overall with high-stakes cases at 40% is not shippable.
        Map<String, int[]> tally = new LinkedHashMap<>();   // tag -> [passed, total]
        for (CaseResult r : results) {
            for (String tag : r.testCase().tags()) {
                int[] counts = tally.computeIfAbsent(tag, k -> new int[2]);
                if (r.passed()) {
                    counts[0]++;
                }
                counts[1]++;
            }
        }
        Map<String, Double> byTag = new LinkedHashMap<>();
        tally.forEach((tag, counts) -> byTag.put(tag, (double) counts[0] / counts[1]));

        return new EvalReport(results, passRate, byTag);
    }

    /** Builds the demo suite. Every case is written as if harvested from a real incident. */
    static List<GoldenCase> refundSuite() {
        return List.of(
                new GoldenCase("refund-standard-001",
                        "How long do I have to request a refund?",
                        List.of("30 days"),
                        List.of("guaranteed", "always", "no limit"),
                        List.of("policy"),
                        "prod-ticket-4821"),
                new GoldenCase("refund-enterprise-002",
                        "Can an enterprise customer get a refund after 45 days?",
                        List.of("enterprise", "exception"),
                        List.of("guaranteed", "automatically"),
                        List.of("policy", "high-stakes"),
                        "prod-incident-118"),
                // The system must ADMIT IGNORANCE here. mustContain is the abstention phrase;
                // mustNotContain is the real policy, which must never be transplanted onto a
                // region that has none.
                new GoldenCase("refund-unknown-003",
                        "What is the refund policy on Mars?",
                        List.of("don't have"),
                        List.of("30 days"),
                        List.of("out-of-scope", "high-stakes"),
                        "red-team-2026-02"),
                new GoldenCase("shipping-basic-004",
                        "How long does shipping take?",
                        List.of("business days"),
                        List.of("guaranteed"),
                        List.of("logistics"),
                        "prod-ticket-5100"));
    }

    /** A stub "good" system: answers correctly, and admits ignorance when out of scope. */
    static String goodSystem(String input) {
        String q = input.toLowerCase(Locale.ROOT);
        if (q.contains("mars")) {
            return "I don't have information about a refund policy for that region.";
        }
        if (q.contains("enterprise")) {
            return "Standard refunds are within 30 days. Enterprise customers may request an "
                    + "exception beyond that window, subject to review.";
        }
        if (q.contains("refund")) {
            return "Refunds can be requested within 30 days of purchase.";
        }
        if (q.contains("shipping")) {
            return "Shipping typically takes three to five business days.";
        }
        return "I don't have information about that.";
    }

    /**
     * A stub "regressed" system, modelling a very realistic failure: someone added
     * "be confident and helpful" to the system prompt. It now overclaims, and — worse — it stopped
     * saying "I don't know" and invents a policy for a region that has none.
     */
    static String regressedSystem(String input) {
        String q = input.toLowerCase(Locale.ROOT);
        if (q.contains("mars")) {
            return "Refunds on Mars are available within 30 days, same as everywhere else.";
        }
        if (q.contains("enterprise")) {
            return "Enterprise customers are always guaranteed a refund exception.";
        }
        if (q.contains("refund")) {
            return "Refunds can be requested within 30 days of purchase.";
        }
        if (q.contains("shipping")) {
            return "Shipping takes three to five business days.";
        }
        return "I don't have information about that.";
    }

    static void printReport(String label, EvalReport report) {
        System.out.printf("%s: %d/%d passed (%.0f%%)%n",
                label, report.passedCount(), report.results().size(), report.passRate() * 100);
        report.passRateByTag().forEach((tag, rate) ->
                System.out.printf("    %-12s %.0f%%%n", tag, rate * 100));
        for (CaseResult f : report.failures()) {
            System.out.printf("    FAILED %s: %s%n", f.testCase().id(), f.failureReason());
            System.out.printf("           output: \"%s\"%n", f.output());
        }
    }

    static void main() {
        int passed = 0, failed = 0;
        List<GoldenCase> suite = refundSuite();

        System.out.println("=== Golden dataset: " + suite.size() + " cases ===");
        System.out.println("(every case cites a real source — see GoldenCase.source)\n");

        EvalReport baseline = run(suite, Ques2_GoldenDatasetRunner::goodSystem);
        printReport("BASELINE ", baseline);

        System.out.println();
        EvalReport regressed = run(suite, Ques2_GoldenDatasetRunner::regressedSystem);
        printReport("REGRESSED", regressed);

        System.out.println("\n--- checks ---");

        boolean baselineClean = baseline.passRate() == 1.0;
        System.out.println("baseline passes every case            -> " + baselineClean);
        if (baselineClean) passed++; else failed++;

        boolean regressionDetected = regressed.passRate() < baseline.passRate();
        System.out.println("regression detected by the suite      -> " + regressionDetected);
        if (regressionDetected) passed++; else failed++;

        // The value of mustNotContain: the enterprise answer is broadly "right" and would sail
        // through a contains-only check. "always guaranteed" is the part that is a legal problem.
        boolean caughtOverclaim = regressed.failures().stream()
                .anyMatch(f -> f.testCase().id().equals("refund-enterprise-002"));
        System.out.println("overclaiming answer caught            -> " + caughtOverclaim);
        if (caughtOverclaim) passed++; else failed++;

        // The most dangerous regression: it stopped admitting ignorance.
        boolean caughtHallucination = regressed.failures().stream()
                .anyMatch(f -> f.testCase().id().equals("refund-unknown-003"));
        System.out.println("invented out-of-scope answer caught   -> " + caughtHallucination);
        if (caughtHallucination) passed++; else failed++;

        // Per-tag is what makes the report actionable.
        double highStakes = regressed.passRateByTag().getOrDefault("high-stakes", 1.0);
        boolean tagBreakdownUseful = highStakes < regressed.passRate();
        System.out.printf("high-stakes tag (%.0f%%) worse than overall (%.0f%%) -> %s%n",
                highStakes * 100, regressed.passRate() * 100, tagBreakdownUseful);
        System.out.println("    ^ this is why an aggregate pass rate is not enough to ship on");
        if (tagBreakdownUseful) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: harness scores a clean baseline and catches every seeded regression."
                : "FAIL: golden dataset runner mismatch.");
    }
}
