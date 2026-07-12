package ra.hul.sdet.multithreading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-Safe Result Collector - Many test threads aggregate PASS/FAIL results with no data loss.
 * Common SDET question: "Aggregate parallel test results safely using ConcurrentHashMap / AtomicInteger."
 *
 * Self-contained (no network). Runs deterministically to completion.
 */
public class Ques2_ThreadSafeResultCollector {

    /** Thread-safe collector: per-status counts in a ConcurrentHashMap plus a total via AtomicInteger. */
    static final class ResultCollector {
        private final Map<String, AtomicInteger> countsByStatus = new ConcurrentHashMap<>();
        private final AtomicInteger total = new AtomicInteger(0);

        void record(String status) {
            // computeIfAbsent is atomic on ConcurrentHashMap; the AtomicInteger handles the increment race.
            countsByStatus.computeIfAbsent(status, k -> new AtomicInteger(0)).incrementAndGet();
            total.incrementAndGet();
        }

        int count(String status) {
            AtomicInteger c = countsByStatus.get(status);
            return c == null ? 0 : c.get();
        }

        int total() {
            return total.get();
        }
    }

    static void main() throws InterruptedException {
        int threads = 8;
        int testsPerThread = 500;
        int expectedTotal = threads * testsPerThread;

        ResultCollector collector = new ResultCollector();
        CountDownLatch startGate = new CountDownLatch(1); // maximise contention: release all at once
        CountDownLatch doneGate = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < testsPerThread; i++) {
                        // Deterministic mix: every 5th test "fails", the rest "pass".
                        String status = ((threadId + i) % 5 == 0) ? "FAIL" : "PASS";
                        collector.record(status);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            }, "test-worker-" + t).start();
        }

        startGate.countDown();
        doneGate.await();

        int pass = collector.count("PASS");
        int fail = collector.count("FAIL");
        int total = collector.total();
        System.out.printf("Aggregated report: PASS=%d FAIL=%d TOTAL=%d (expected %d)%n", pass, fail, total, expectedTotal);

        if (total == expectedTotal && pass + fail == expectedTotal) {
            System.out.println("PASSED: no results lost under concurrent aggregation.");
        } else {
            System.out.println("FAILED: expected " + expectedTotal + " results, got " + total);
        }
    }
}
