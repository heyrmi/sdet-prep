package ra.hul.sdet.builds;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Retry Framework - Generic, reusable retry for flaky operations.
 * Common SDET question (machine-coding round): "Build a retry mechanism: configurable max attempts,
 * delay, exponential backoff, retry only on given exception types; functional interface; log attempts;
 * return result or throw after exhaustion."
 *
 * Self-contained: exercises the retry engine against flaky lambdas in main() (no real sleeping delays).
 */
public class Ques6_RetryFramework {

    /** An operation that may fail; retried on configured exceptions. */
    @FunctionalInterface
    public interface RetryableTask<T> {
        T run() throws Exception;
    }

    /** Immutable retry configuration built via a small fluent builder. */
    public static final class RetryPolicy {
        final int maxAttempts;
        final long baseDelayMs;
        final boolean exponentialBackoff;
        final Set<Class<? extends Throwable>> retryOn;

        private RetryPolicy(int maxAttempts, long baseDelayMs, boolean expo,
                            Set<Class<? extends Throwable>> retryOn) {
            this.maxAttempts = maxAttempts;
            this.baseDelayMs = baseDelayMs;
            this.exponentialBackoff = expo;
            this.retryOn = retryOn;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private int maxAttempts = 3;
            private long baseDelayMs = 0;
            private boolean expo = false;
            private Set<Class<? extends Throwable>> retryOn = Set.of(Exception.class);

            public Builder maxAttempts(int n) { this.maxAttempts = n; return this; }
            public Builder delayMs(long ms) { this.baseDelayMs = ms; return this; }
            public Builder exponentialBackoff(boolean b) { this.expo = b; return this; }
            @SafeVarargs
            public final Builder retryOn(Class<? extends Throwable>... types) {
                this.retryOn = Set.of(types); return this;
            }
            public RetryPolicy build() { return new RetryPolicy(maxAttempts, baseDelayMs, expo, retryOn); }
        }

        boolean shouldRetry(Throwable t) {
            return retryOn.stream().anyMatch(c -> c.isInstance(t));
        }

        long delayForAttempt(int attempt) { // attempt is 1-based
            return exponentialBackoff ? baseDelayMs * (1L << (attempt - 1)) : baseDelayMs;
        }
    }

    /** Captures attempt-by-attempt history so callers/tests can inspect what happened. */
    public record RetryOutcome<T>(T result, int attempts, List<String> log) {}

    /** Execute the task under the policy. Retries on matching exceptions until success or exhaustion. */
    public static <T> RetryOutcome<T> execute(RetryPolicy policy, RetryableTask<T> task) throws Exception {
        List<String> log = new ArrayList<>();
        Throwable last = null;
        for (int attempt = 1; attempt <= policy.maxAttempts; attempt++) {
            try {
                T result = task.run();
                log.add("attempt " + attempt + ": SUCCESS");
                return new RetryOutcome<>(result, attempt, log);
            } catch (Exception e) {
                last = e;
                boolean willRetry = attempt < policy.maxAttempts && policy.shouldRetry(e);
                long delay = willRetry ? policy.delayForAttempt(attempt) : 0;
                log.add("attempt " + attempt + ": FAILED (" + e.getClass().getSimpleName() + ": "
                        + e.getMessage() + ")" + (willRetry ? " -> retry after " + delay + "ms" : " -> giving up"));
                if (!policy.shouldRetry(e)) throw e;       // non-retryable: fail fast
                if (attempt >= policy.maxAttempts) break;   // exhausted
                if (delay > 0) Thread.sleep(delay);
            }
        }
        throw new RetryException("Exhausted " + policy.maxAttempts + " attempts", last);
    }

    /** Thrown when all retries are used up. */
    public static class RetryException extends Exception {
        public RetryException(String msg, Throwable cause) { super(msg, cause); }
    }

    static void main() throws Exception {
        System.out.println("=== Retry Framework ===");

        // Case 1: flaky task succeeds on the 3rd attempt.
        int[] calls = {0};
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(5).delayMs(1).exponentialBackoff(true)
                .retryOn(IllegalStateException.class).build();
        RetryOutcome<String> ok = execute(policy, () -> {
            calls[0]++;
            if (calls[0] < 3) throw new IllegalStateException("service warming up");
            return "OK-after-" + calls[0];
        });
        ok.log().forEach(l -> System.out.println("  " + l));
        System.out.println("Result: " + ok.result() + " in " + ok.attempts() + " attempts");

        // Case 2: non-retryable exception fails fast (only 1 attempt).
        int[] c2 = {0};
        boolean failedFast = false;
        try {
            execute(policy, () -> { c2[0]++; throw new IllegalArgumentException("bad input"); });
        } catch (IllegalArgumentException e) {
            failedFast = c2[0] == 1;
            System.out.println("Non-retryable threw immediately after " + c2[0] + " attempt.");
        }

        // Case 3: always fails -> exhausts and throws RetryException.
        int[] c3 = {0};
        boolean exhausted = false;
        RetryPolicy p3 = RetryPolicy.builder().maxAttempts(3).delayMs(0)
                .retryOn(RuntimeException.class).build();
        try {
            execute(p3, () -> { c3[0]++; throw new RuntimeException("down"); });
        } catch (RetryException e) {
            exhausted = c3[0] == 3;
            System.out.println("Exhausted after " + c3[0] + " attempts: " + e.getMessage());
        }

        boolean pass = ok.attempts() == 3 && ok.result().equals("OK-after-3") && failedFast && exhausted;
        System.out.println(pass
                ? "PASSED: retry succeeds on 3rd, fails fast on non-retryable, exhausts after max."
                : "FAILED: retry behavior mismatch.");
    }
}
