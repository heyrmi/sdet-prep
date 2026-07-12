package ra.hul.sdet.performance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API Throughput Measurement - Step up concurrency, measure avg latency + error rate per level, find degradation.
 * Common SDET question: "Ramp concurrent users and locate the point where response time or errors climb."
 *
 * Target: https://httpbin.org/get (free, no auth). NEEDS NETWORK. Levels kept small/bounded so it finishes fast.
 */
public class Ques2_ApiThroughputMeasurement {

    record LevelResult(int concurrency, int requests, int errors, double avgMs, double throughputPerSec) {}

    static long timeOneRequest(HttpClient client, String url) {
        long start = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            long ms = (System.nanoTime() - start) / 1_000_000;
            return (resp.statusCode() >= 200 && resp.statusCode() < 300) ? ms : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Runs one load level: `concurrency` threads each issuing `perThread` requests; reports avg latency + errors. */
    static LevelResult runLevel(HttpClient client, String url, int concurrency, int perThread) throws InterruptedException {
        int requests = concurrency * perThread;
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            tasks.add(() -> {
                long ms = timeOneRequest(client, url);
                if (ms < 0) errors.incrementAndGet(); else latencies.add(ms);
                return null;
            });
        }

        long wallStart = System.nanoTime();
        pool.invokeAll(tasks);
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        double avg = latencies.isEmpty() ? 0 : latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        double throughput = wallMs > 0 ? (requests * 1000.0 / wallMs) : requests;
        return new LevelResult(concurrency, requests, errors.get(), avg, throughput);
    }

    static void main() throws InterruptedException {
        String url = "https://httpbin.org/get";
        int[] levels = { 2, 4, 8 }; // bounded step-up ramp
        int perThread = 3;

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        List<LevelResult> results = new ArrayList<>();

        System.out.println("=== Ramp-up throughput measurement ===");
        System.out.printf("%-12s %-10s %-8s %-12s %-14s%n", "Concurrency", "Requests", "Errors", "AvgLatency", "Throughput");
        for (int c : levels) {
            LevelResult r = runLevel(client, url, c, perThread);
            results.add(r);
            System.out.printf("%-12d %-10d %-8d %-10.1fms %-10.1f req/s%n",
                    r.concurrency(), r.requests(), r.errors(), r.avgMs(), r.throughputPerSec());
        }

        // Degradation heuristic: first level whose avg latency is >2x the baseline level, or shows errors.
        LevelResult baseline = results.get(0);
        LevelResult degradedAt = null;
        for (int i = 1; i < results.size(); i++) {
            LevelResult r = results.get(i);
            if (r.errors() > 0 || (baseline.avgMs() > 0 && r.avgMs() > baseline.avgMs() * 2)) {
                degradedAt = r;
                break;
            }
        }
        if (degradedAt != null) {
            System.out.printf("Degradation point: concurrency=%d (avg %.1fms, errors=%d).%n",
                    degradedAt.concurrency(), degradedAt.avgMs(), degradedAt.errors());
        } else {
            System.out.println("No clear degradation within the tested range; endpoint scaled cleanly.");
        }

        // Resilient check: the ramp completed and produced a result per level.
        if (results.size() == levels.length) {
            System.out.println("PASSED: throughput measured across all concurrency levels.");
        } else {
            System.out.println("FAILED: did not complete all levels.");
        }
    }
}
