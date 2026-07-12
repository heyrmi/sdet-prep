package ra.hul.sdet.api;

import io.restassured.RestAssured;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Response Time Percentiles - Repeatedly call an endpoint, collect latencies, and compute
 * min / max / avg / P95 / P99. Fail if P95 exceeds a (generous) threshold.
 * Common SDET question: "Measure response times over many calls and assert aggregated
 * percentile metrics rather than a single-call timing."
 *
 * Target: https://jsonplaceholder.typicode.com (free, no auth). NEEDS NETWORK.
 * Threshold is intentionally generous to stay non-flaky on public infrastructure.
 */
public class Ques6_ResponseTimePercentiles {

    record Stats(long min, long max, double avg, long p95, long p99) {}

    /** Nearest-rank percentile over a copy of the samples (0.0..1.0). */
    static long percentile(List<Long> samplesMs, double p) {
        List<Long> sorted = new ArrayList<>(samplesMs);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(p * sorted.size());     // nearest-rank
        int idx = Math.min(Math.max(rank - 1, 0), sorted.size() - 1);
        return sorted.get(idx);
    }

    static Stats computeStats(List<Long> samplesMs) {
        long min = Collections.min(samplesMs);
        long max = Collections.max(samplesMs);
        double avg = samplesMs.stream().mapToLong(Long::longValue).average().orElse(0);
        return new Stats(min, max, avg, percentile(samplesMs, 0.95), percentile(samplesMs, 0.99));
    }

    static void main() {
        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        final int calls = 20;
        final long p95ThresholdMs = 3000; // generous for public API

        System.out.printf("=== Sampling response time over %d GET /posts/1 calls ===%n", calls);
        List<Long> latencies = new ArrayList<>(calls);
        for (int i = 0; i < calls; i++) {
            long ms = given().when().get("/posts/1").time(); // RestAssured measured time (ms)
            latencies.add(ms);
        }

        Stats s = computeStats(latencies);
        System.out.printf("min=%dms  max=%dms  avg=%.1fms  P95=%dms  P99=%dms%n",
                s.min(), s.max(), s.avg(), s.p95(), s.p99());

        boolean ok = s.p95() <= p95ThresholdMs;
        System.out.println(ok
                ? "PASSED: P95 (" + s.p95() + "ms) within threshold " + p95ThresholdMs + "ms."
                : "FAILED: P95 (" + s.p95() + "ms) exceeded threshold " + p95ThresholdMs + "ms.");
    }
}
