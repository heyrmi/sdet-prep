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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Load Test - Fire N concurrent HTTP requests and report throughput + latency percentiles.
 * Common SDET question: "Build a mini load tester: throughput, min/max/avg/P95/P99, error rate."
 *
 * Target: https://httpbin.org/get (free, no auth). NEEDS NETWORK. N kept small (20) so it finishes fast.
 */
public class Ques1_SimpleLoadTest {

    record LoadStats(int total, int errors, double throughputPerSec,
                     long minMs, long maxMs, double avgMs, long p95Ms, long p99Ms) {}

    /** One request: returns latency in ms, or -1 on error (non-2xx or exception). */
    static long timeOneRequest(HttpClient client, String url) {
        long start = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            long ms = (System.nanoTime() - start) / 1_000_000;
            return (resp.statusCode() >= 200 && resp.statusCode() < 300) ? ms : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Runs `requests` calls across a pool of `concurrency` threads, gathering latencies and computing stats. */
    static LoadStats runLoad(String url, int requests, int concurrency) throws InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            tasks.add(() -> {
                long ms = timeOneRequest(client, url);
                if (ms < 0) {
                    errors.incrementAndGet();
                } else {
                    latencies.add(ms);
                }
                return null;
            });
        }

        long wallStart = System.nanoTime();
        List<Future<Void>> futures = pool.invokeAll(tasks); // blocks until all complete
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        for (Future<Void> f : futures) {
            try { f.get(); } catch (Exception ignored) { /* already counted as error */ }
        }

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int ok = sorted.size();
        double throughput = wallMs > 0 ? (requests * 1000.0 / wallMs) : requests;

        long min = ok == 0 ? 0 : sorted.get(0);
        long max = ok == 0 ? 0 : sorted.get(ok - 1);
        double avg = ok == 0 ? 0 : sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = percentile(sorted, 95);
        long p99 = percentile(sorted, 99);
        return new LoadStats(requests, errors.get(), throughput, min, max, avg, p95, p99);
    }

    /** Nearest-rank percentile over an already-sorted list. */
    static long percentile(List<Long> sorted, int pct) {
        if (sorted.isEmpty()) return 0;
        int rank = (int) Math.ceil(pct / 100.0 * sorted.size());
        return sorted.get(Math.min(rank, sorted.size()) - 1);
    }

    static void main() throws InterruptedException {
        String url = "https://httpbin.org/get";
        int requests = 20;
        int concurrency = 5;

        System.out.printf("Load testing %s: %d requests, concurrency %d ...%n", url, requests, concurrency);
        LoadStats s = runLoad(url, requests, concurrency);

        System.out.println("=== Results ===");
        System.out.printf("  Requests   : %d (errors: %d)%n", s.total(), s.errors());
        System.out.printf("  Throughput : %.1f req/sec%n", s.throughputPerSec());
        System.out.printf("  Latency ms : min=%d avg=%.1f p95=%d p99=%d max=%d%n",
                s.minMs(), s.avgMs(), s.p95Ms(), s.p99Ms(), s.maxMs());

        // Resilient check: don't assert on volatile latency numbers, just that the harness ran and mostly succeeded.
        if (s.errors() <= s.total() / 2) {
            System.out.println("PASSED: load test completed and reported metrics.");
        } else {
            System.out.println("FAILED: majority of requests errored (network issue?).");
        }
    }
}
