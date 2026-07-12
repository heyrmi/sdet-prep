package ra.hul.sdet.multithreading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Parallel Test Executor - Run a list of Callable "tests" on a fixed thread pool, collecting results.
 * Common SDET question: "Build a parallel test runner with ExecutorService that handles timeout and failure."
 *
 * Self-contained (no network). Runs deterministically to completion.
 */
public class Ques3_ParallelTestExecutor {

    /** Outcome of a single test run. */
    enum Outcome { PASSED, FAILED, TIMED_OUT }

    record TestResult(String name, Outcome outcome, String detail) {}

    /** Submits every test, then collects each Future with a per-test timeout, mapping exceptions to outcomes. */
    static List<TestResult> runAll(List<Callable<Boolean>> tests, List<String> names, int poolSize, long timeoutMs) {
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> test : tests) {
            futures.add(pool.submit(test));
        }

        List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            String name = names.get(i);
            try {
                boolean ok = futures.get(i).get(timeoutMs, TimeUnit.MILLISECONDS);
                results.add(new TestResult(name, ok ? Outcome.PASSED : Outcome.FAILED, ok ? "ok" : "assertion returned false"));
            } catch (TimeoutException e) {
                futures.get(i).cancel(true); // interrupt the slow test
                results.add(new TestResult(name, Outcome.TIMED_OUT, "exceeded " + timeoutMs + "ms"));
            } catch (ExecutionException e) {
                results.add(new TestResult(name, Outcome.FAILED, "threw " + e.getCause().getClass().getSimpleName()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(new TestResult(name, Outcome.FAILED, "interrupted"));
            }
        }
        pool.shutdownNow();
        return results;
    }

    static void main() {
        List<Callable<Boolean>> tests = new ArrayList<>();
        List<String> names = new ArrayList<>();

        // 1) fast pass
        names.add("test_fast_pass");
        tests.add(() -> { Thread.sleep(50); return true; });
        // 2) assertion failure
        names.add("test_assertion_fail");
        tests.add(() -> { Thread.sleep(50); return false; });
        // 3) throws an exception
        names.add("test_throws");
        tests.add(() -> { throw new IllegalStateException("boom"); });
        // 4) too slow -> timeout
        names.add("test_timeout");
        tests.add(() -> { Thread.sleep(2000); return true; });
        // 5) another pass
        names.add("test_slow_pass");
        tests.add(() -> { Thread.sleep(100); return true; });

        List<TestResult> results = runAll(tests, names, 3, 500);

        long passed = 0, failed = 0, timedOut = 0;
        for (TestResult r : results) {
            System.out.printf("  %-22s %-10s (%s)%n", r.name(), r.outcome(), r.detail());
            switch (r.outcome()) {
                case PASSED -> passed++;
                case FAILED -> failed++;
                case TIMED_OUT -> timedOut++;
            }
        }
        System.out.printf("Summary: %d passed, %d failed, %d timed out.%n", passed, failed, timedOut);

        if (passed == 2 && failed == 2 && timedOut == 1) {
            System.out.println("PASSED: executor collected all outcomes and handled timeout + exception.");
        } else {
            System.out.println("FAILED: unexpected outcome distribution.");
        }
    }
}
