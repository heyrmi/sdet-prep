package ra.hul.verify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a problem's printed output represents a pass or a failure.
 *
 * <p>This is the verifier's <b>oracle</b>, and it is extracted here for a reason: it is the one
 * piece of the harness that can be wrong in a way nothing else notices. A bug in the runner makes
 * the build fail loudly; a bug in the oracle makes the build pass silently while problems rot.
 *
 * <p>So it is plain, dependency-free logic with its own unit tests, and it is the target of the
 * {@code -Pmutation} profile — because "is our test of the tests any good?" is exactly the
 * question mutation testing answers.
 *
 * <p>The repo convention it encodes:
 * <ul>
 *   <li>{@code PASSED: ...} marks a satisfied self-check.</li>
 *   <li>{@code FAIL: ...} / {@code FAILED: ...} / {@code ERROR: ...} at the start of a line marks
 *       a failed one.</li>
 *   <li>{@code === N passed, M failed ===} is an aggregate summary; M &gt; 0 is a failure.</li>
 *   <li>Empty output is a failure — a self-verifying problem that prints nothing verified nothing.</li>
 * </ul>
 */
public final class OutcomeAnalyzer {

    /**
     * A failure line, anchored to the start of a line and requiring the colon.
     *
     * <p>Anchoring is not a stylistic choice. A bare substring search for {@code FAILED} matches
     * narrative output like {@code "attempt 1: FAILED -> retry after 1ms"} in
     * {@code builds/Ques6_RetryFramework}, which is a <em>passing</em> problem describing a retry.
     * That false positive is the reason this pattern exists in this form.
     */
    private static final Pattern FAILURE_LINE =
            Pattern.compile("^\\s*(FAIL|FAILED|ERROR):", Pattern.MULTILINE);

    /** The {@code === N passed, M failed ===} summary some problems print. */
    private static final Pattern SUMMARY_LINE =
            Pattern.compile("===\\s*(\\d+)\\s+passed,\\s*(\\d+)\\s+failed\\s*===");

    private OutcomeAnalyzer() {
    }

    /** Why a problem's output was rejected, or {@link #OK}. */
    public enum Verdict {
        OK,
        NO_OUTPUT,
        FAILURE_LINE_PRESENT,
        SUMMARY_REPORTS_FAILURES
    }

    /** Analyses captured stdout/stderr from one problem run. */
    public static Verdict analyze(String output) {
        if (output == null || output.isBlank()) {
            return Verdict.NO_OUTPUT;
        }
        if (FAILURE_LINE.matcher(output).find()) {
            return Verdict.FAILURE_LINE_PRESENT;
        }
        if (failedCount(output) > 0) {
            return Verdict.SUMMARY_REPORTS_FAILURES;
        }
        return Verdict.OK;
    }

    /**
     * Total failures reported across every {@code === N passed, M failed ===} summary in the
     * output, or 0 if there is none. Problems may print more than one summary, so all are summed
     * rather than only the first.
     */
    public static int failedCount(String output) {
        if (output == null) {
            return 0;
        }
        Matcher m = SUMMARY_LINE.matcher(output);
        int total = 0;
        while (m.find()) {
            total += Integer.parseInt(m.group(2));
        }
        return total;
    }

    /** Total passes reported across every summary, or 0 if there is none. */
    public static int passedCount(String output) {
        if (output == null) {
            return 0;
        }
        Matcher m = SUMMARY_LINE.matcher(output);
        int total = 0;
        while (m.find()) {
            total += Integer.parseInt(m.group(1));
        }
        return total;
    }

    /** Human-readable explanation for a rejected verdict. */
    public static String describe(Verdict verdict, String className) {
        return switch (verdict) {
            case OK -> className + " passed";
            case NO_OUTPUT -> className
                    + " produced no output — a self-verifying problem must print its results.";
            case FAILURE_LINE_PRESENT -> className + " reported a failed self-check.";
            case SUMMARY_REPORTS_FAILURES -> className + " summary reports failed assertion(s).";
        };
    }
}
