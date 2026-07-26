package ra.hul.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the verifier's oracle.
 *
 * <p>These are the tests the {@code -Pmutation} profile mutates. If a mutant survives — pitest
 * changes the logic and nothing here notices — that is a real gap in the oracle, and the oracle
 * is the one component whose bugs are silent.
 */
class OutcomeAnalyzerTest {

    @Test
    @DisplayName("blank output is rejected")
    void blankOutput() {
        assertEquals(OutcomeAnalyzer.Verdict.NO_OUTPUT, OutcomeAnalyzer.analyze(""));
        assertEquals(OutcomeAnalyzer.Verdict.NO_OUTPUT, OutcomeAnalyzer.analyze("   \n  \t "));
        assertEquals(OutcomeAnalyzer.Verdict.NO_OUTPUT, OutcomeAnalyzer.analyze(null));
    }

    @Test
    @DisplayName("a clean PASSED run is accepted")
    void passingRun() {
        assertEquals(OutcomeAnalyzer.Verdict.OK,
                OutcomeAnalyzer.analyze("[0, 1]\nPASSED: two-sum returns the expected indices."));
    }

    @Test
    @DisplayName("a FAIL: line is rejected")
    void failureLine() {
        assertEquals(OutcomeAnalyzer.Verdict.FAILURE_LINE_PRESENT,
                OutcomeAnalyzer.analyze("some output\nFAIL: CSV parsing mismatch."));
    }

    @Test
    @DisplayName("FAILED: and ERROR: prefixes are also rejected")
    void otherFailurePrefixes() {
        assertEquals(OutcomeAnalyzer.Verdict.FAILURE_LINE_PRESENT,
                OutcomeAnalyzer.analyze("FAILED: something broke"));
        assertEquals(OutcomeAnalyzer.Verdict.FAILURE_LINE_PRESENT,
                OutcomeAnalyzer.analyze("ERROR: something broke"));
    }

    @Test
    @DisplayName("a failure line is detected with leading whitespace")
    void indentedFailureLine() {
        assertEquals(OutcomeAnalyzer.Verdict.FAILURE_LINE_PRESENT,
                OutcomeAnalyzer.analyze("header\n    FAIL: nested check failed"));
    }

    @Test
    @DisplayName("narrative FAILED mid-line is NOT a failure")
    void narrativeFailedIsNotAFailure() {
        // The exact false positive this design exists to avoid: Ques6_RetryFramework passes
        // while describing failed attempts.
        String retryOutput = """
                === Retry Framework ===
                  attempt 1: FAILED (IllegalStateException) -> retry after 1ms
                  attempt 2: FAILED (IllegalStateException) -> retry after 2ms
                  attempt 3: SUCCESS
                PASSED: retry succeeds on the 3rd attempt.
                """;
        assertEquals(OutcomeAnalyzer.Verdict.OK, OutcomeAnalyzer.analyze(retryOutput));
    }

    @Test
    @DisplayName("a zero-failure summary is accepted")
    void cleanSummary() {
        assertEquals(OutcomeAnalyzer.Verdict.OK,
                OutcomeAnalyzer.analyze("=== 4 passed, 0 failed ===\nPASSED: all good."));
    }

    @Test
    @DisplayName("a summary reporting failures is rejected")
    void failingSummary() {
        assertEquals(OutcomeAnalyzer.Verdict.SUMMARY_REPORTS_FAILURES,
                OutcomeAnalyzer.analyze("=== 3 passed, 2 failed ==="));
    }

    @Test
    @DisplayName("failures are summed across multiple summaries")
    void multipleSummaries() {
        String output = "=== 3 passed, 0 failed ===\nmore work\n=== 1 passed, 4 failed ===";
        assertEquals(4, OutcomeAnalyzer.failedCount(output));
        assertEquals(4, OutcomeAnalyzer.passedCount(output));
        assertEquals(OutcomeAnalyzer.Verdict.SUMMARY_REPORTS_FAILURES,
                OutcomeAnalyzer.analyze(output));
    }

    @Test
    @DisplayName("output with no summary reports zero counts")
    void noSummary() {
        assertEquals(0, OutcomeAnalyzer.failedCount("just some text"));
        assertEquals(0, OutcomeAnalyzer.passedCount("just some text"));
        assertEquals(0, OutcomeAnalyzer.failedCount(null));
        assertEquals(0, OutcomeAnalyzer.passedCount(null));
    }

    @Test
    @DisplayName("summary parsing tolerates spacing variations")
    void summarySpacing() {
        assertEquals(2, OutcomeAnalyzer.failedCount("===  5  passed,  2  failed  ==="));
    }

    @Test
    @DisplayName("a failure line outranks a clean summary")
    void failureLineTakesPrecedence() {
        // Both signals present: the explicit FAIL: line is the more specific one and must win,
        // so the message points at the actual failed check.
        assertEquals(OutcomeAnalyzer.Verdict.FAILURE_LINE_PRESENT,
                OutcomeAnalyzer.analyze("FAIL: boom\n=== 5 passed, 0 failed ==="));
    }

    @Test
    @DisplayName("describe produces a distinct message per verdict")
    void describeMessages() {
        for (OutcomeAnalyzer.Verdict v : OutcomeAnalyzer.Verdict.values()) {
            String message = OutcomeAnalyzer.describe(v, "Ques1_Example");
            assertEquals(true, message.contains("Ques1_Example"),
                    "message for " + v + " must name the class");
        }
    }
}
