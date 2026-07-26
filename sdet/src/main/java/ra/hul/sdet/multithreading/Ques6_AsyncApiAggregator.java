package ra.hul.sdet.multithreading;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Async API Call Aggregator - fan out to several services, assemble one response.
 *
 * <p>The "build a dashboard that pulls from five services" problem. Sequentially it takes the SUM
 * of the latencies; concurrently it takes the MAX. That much is obvious. The interesting parts,
 * and what an interviewer actually probes:
 *
 * <ul>
 *   <li><b>Per-call timeouts</b>, so one slow service cannot hold the whole response hostage.</li>
 *   <li><b>Partial results.</b> If the recommendations service is down, you still render the
 *       page without recommendations. Failing the entire request because one optional
 *       dependency failed is the most common real-world mistake here.</li>
 *   <li><b>Never block inside the async chain.</b> Calling {@code join()} on a future while
 *       running on the same pool that must complete it is a classic self-deadlock.</li>
 * </ul>
 *
 * <p>Uses {@link CompletableFuture} with an explicit executor — relying on the common ForkJoinPool
 * for blocking I/O starves every other user of it, which is a real production incident and worth
 * being able to explain.
 *
 * <p>Self-contained: services are simulated with configurable latency and failure, no network.
 */
public class Ques6_AsyncApiAggregator {

    /** One simulated downstream service. */
    record Service(String name, Duration latency, boolean fails, boolean optional) {
    }

    /** The outcome of one downstream call. */
    record CallResult(String service, String value, String error, long millis) {
        boolean ok() {
            return error == null;
        }
    }

    /** The assembled response. */
    record Aggregate(Map<String, String> data, List<String> failures, long totalMillis) {
        boolean degraded() {
            return !failures.isEmpty();
        }
    }

    static final AtomicInteger CALLS = new AtomicInteger();

    /** Simulates calling a service. Throws for a failing one. */
    static String call(Service s) {
        CALLS.incrementAndGet();
        try {
            Thread.sleep(s.latency().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted");
        }
        if (s.fails()) {
            throw new IllegalStateException(s.name() + " is unavailable");
        }
        return s.name() + "-payload";
    }

    /** Sequential baseline: the sum of every latency. */
    static Aggregate aggregateSequentially(List<Service> services) {
        long start = System.nanoTime();
        Map<String, String> data = new java.util.LinkedHashMap<>();
        List<String> failures = new ArrayList<>();

        for (Service s : services) {
            try {
                data.put(s.name(), call(s));
            } catch (RuntimeException e) {
                failures.add(s.name());
            }
        }
        return new Aggregate(data, failures, (System.nanoTime() - start) / 1_000_000);
    }

    /**
     * Concurrent aggregation with per-call timeouts and graceful degradation.
     *
     * @param perCallTimeout applied to each service independently
     */
    static Aggregate aggregateConcurrently(List<Service> services, Duration perCallTimeout,
                                           ExecutorService pool) {
        long start = System.nanoTime();

        List<CompletableFuture<CallResult>> futures = new ArrayList<>();
        for (Service s : services) {
            CompletableFuture<CallResult> f = CompletableFuture
                    // Explicit executor: using the common ForkJoinPool for blocking I/O starves
                    // every other consumer of it, including parallel streams elsewhere.
                    .supplyAsync(() -> {
                        long t0 = System.nanoTime();
                        String value = call(s);
                        return new CallResult(s.name(), value, null, (System.nanoTime() - t0) / 1_000_000);
                    }, pool)
                    .orTimeout(perCallTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    // One slow or broken service must not sink the whole response.
                    .exceptionally(ex -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String reason = cause instanceof TimeoutException
                                ? "timeout after " + perCallTimeout.toMillis() + "ms"
                                : cause.getMessage();
                        return new CallResult(s.name(), null, reason, perCallTimeout.toMillis());
                    });
            futures.add(f);
        }

        // Wait for everything, then assemble. allOf never completes exceptionally here because
        // each future already handled its own failure above.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, String> data = new java.util.LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        for (CompletableFuture<CallResult> f : futures) {
            CallResult r = f.join();
            if (r.ok()) {
                data.put(r.service(), r.value());
            } else {
                failures.add(r.service() + " (" + r.error() + ")");
            }
        }
        return new Aggregate(data, failures, (System.nanoTime() - start) / 1_000_000);
    }

    static List<Service> healthyServices() {
        return List.of(
                new Service("profile", Duration.ofMillis(100), false, false),
                new Service("orders", Duration.ofMillis(120), false, false),
                new Service("recommendations", Duration.ofMillis(90), false, true),
                new Service("inventory", Duration.ofMillis(110), false, false),
                new Service("reviews", Duration.ofMillis(80), false, true));
    }

    static void main() {
        int passed = 0, failed = 0;
        ExecutorService pool = Executors.newFixedThreadPool(8);

        try {
            System.out.println("=== Fan-out aggregation across 5 services ===\n");

            CALLS.set(0);
            Aggregate seq = aggregateSequentially(healthyServices());
            System.out.printf("sequential : %d services in %4d ms  (sum of latencies)%n",
                    seq.data().size(), seq.totalMillis());

            CALLS.set(0);
            Aggregate conc = aggregateConcurrently(healthyServices(), Duration.ofMillis(500), pool);
            System.out.printf("concurrent : %d services in %4d ms  (max of latencies)%n",
                    conc.data().size(), conc.totalMillis());

            System.out.println("\n--- checks ---");

            boolean c1 = seq.data().size() == 5 && conc.data().size() == 5;
            System.out.println("both paths return all 5 payloads            : " + c1);
            if (c1) passed++; else failed++;

            boolean c2 = conc.totalMillis() < seq.totalMillis();
            System.out.printf("concurrent beats sequential (%dms < %dms)  : %s%n",
                    conc.totalMillis(), seq.totalMillis(), c2);
            if (c2) passed++; else failed++;

            // Concurrent time should be near the SLOWEST call (120ms), not the sum (500ms).
            boolean c3 = conc.totalMillis() < 300;
            System.out.printf("concurrent time ~ max latency, not the sum  : %s%n", c3);
            if (c3) passed++; else failed++;

            // --- Graceful degradation ---
            System.out.println("\n=== One optional service is down ===");
            List<Service> degraded = List.of(
                    new Service("profile", Duration.ofMillis(50), false, false),
                    new Service("orders", Duration.ofMillis(60), false, false),
                    new Service("recommendations", Duration.ofMillis(40), true, true));   // fails

            Aggregate partial = aggregateConcurrently(degraded, Duration.ofMillis(500), pool);
            System.out.println("  returned: " + partial.data().keySet());
            System.out.println("  failed  : " + partial.failures());

            boolean c4 = partial.data().size() == 2 && partial.failures().size() == 1;
            System.out.println("partial result returned, not a total failure: " + c4);
            System.out.println("    ^ failing the whole page because recommendations died is");
            System.out.println("      the most common mistake in this problem");
            if (c4) passed++; else failed++;

            boolean c5 = partial.degraded();
            System.out.println("response is flagged as degraded             : " + c5);
            System.out.println("    ^ silently serving a partial response as if it were complete");
            System.out.println("      is worse than the failure itself");
            if (c5) passed++; else failed++;

            // --- Timeout isolation ---
            System.out.println("\n=== One service is very slow ===");
            List<Service> slow = List.of(
                    new Service("fast-a", Duration.ofMillis(30), false, false),
                    new Service("fast-b", Duration.ofMillis(40), false, false),
                    new Service("glacial", Duration.ofSeconds(5), false, true));

            Aggregate timed = aggregateConcurrently(slow, Duration.ofMillis(200), pool);
            System.out.printf("  completed in %d ms with %d payloads%n",
                    timed.totalMillis(), timed.data().size());

            boolean c6 = timed.totalMillis() < 1000 && timed.data().size() == 2;
            System.out.println("slow service timed out without blocking     : " + c6);
            System.out.println("    ^ without a per-call timeout this response takes 5 SECONDS");
            if (c6) passed++; else failed++;

            boolean c7 = timed.failures().stream().anyMatch(f -> f.contains("timeout"));
            System.out.println("timeout reported distinctly from an error   : " + c7);
            if (c7) passed++; else failed++;

        } finally {
            pool.shutdownNow();
        }

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: fan-out is concurrent, timeout-isolated, and degrades gracefully."
                : "FAIL: async aggregator mismatch.");
    }
}
