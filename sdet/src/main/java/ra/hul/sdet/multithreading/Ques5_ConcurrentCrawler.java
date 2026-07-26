package ra.hul.sdet.multithreading;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent Web Crawler - the classic concurrency interview problem, without the network.
 *
 * <p>Crawl a link graph with N workers. The three things an interviewer is checking:
 *
 * <ol>
 *   <li><b>Visit each page exactly once</b> under concurrency. The naive
 *       {@code if (!visited.contains(u)) { visited.add(u); ... }} is a check-then-act race: two
 *       threads both pass the check before either adds. The fix is one atomic operation —
 *       {@code visited.add(u)} on a concurrent set returns false if it was already there.</li>
 *   <li><b>Know when you are done.</b> There is no fixed task count: each page discovers more.
 *       An ordinary {@code awaitTermination} deadlocks because workers are still submitting.</li>
 *   <li><b>Stay bounded.</b> A cyclic graph must not loop forever, and depth must be capped.</li>
 * </ol>
 *
 * <p>Termination is done here with a {@link Phaser}: register before submitting a task, arrive
 * when it finishes, and the party count reaching zero means the crawl is genuinely complete.
 * A {@code CountDownLatch} cannot express this because the count is not known in advance.
 *
 * <p>Self-contained: the "web" is an in-memory link graph with a deliberate cycle, so the problem
 * runs offline and deterministically.
 */
public class Ques5_ConcurrentCrawler {

    /** An in-memory stand-in for the network. Fetching costs a little time, as it would live. */
    static final class FakeWeb {
        private final Map<String, List<String>> links;
        private final AtomicInteger fetchCount = new AtomicInteger();

        FakeWeb(Map<String, List<String>> links) {
            this.links = links;
        }

        List<String> fetch(String url) {
            fetchCount.incrementAndGet();
            try {
                Thread.sleep(5);   // simulate latency so the workers genuinely overlap
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return links.getOrDefault(url, List.of());
        }

        int fetchCount() {
            return fetchCount.get();
        }
    }

    /** The crawl result. */
    record CrawlResult(Set<String> visited, int fetches, long millis) {
    }

    /**
     * Crawls from {@code seed} with {@code workers} threads, up to {@code maxDepth} hops.
     *
     * @param maxDepth 0 means "seed only"
     */
    static CrawlResult crawl(FakeWeb web, String seed, int workers, int maxDepth) {
        // A concurrent set whose add() is atomic — this is what removes the check-then-act race.
        Set<String> visited = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        // The crawl has no known task count: every page discovers more work. A Phaser tracks
        // outstanding work dynamically, which a CountDownLatch cannot do.
        Phaser phaser = new Phaser(1);   // the caller itself is party 1

        long start = System.nanoTime();
        if (visited.add(seed)) {
            submit(pool, phaser, web, visited, seed, 0, maxDepth);
        }
        phaser.arriveAndAwaitAdvance();   // wait for every submitted task to finish

        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        return new CrawlResult(Collections.unmodifiableSet(new HashSet<>(visited)),
                web.fetchCount(), millis);
    }

    private static void submit(ExecutorService pool, Phaser phaser, FakeWeb web,
                               Set<String> visited, String url, int depth, int maxDepth) {
        phaser.register();   // register BEFORE submitting, or the crawl can finish early
        pool.execute(() -> {
            try {
                if (depth >= maxDepth) {
                    return;
                }
                for (String next : web.fetch(url)) {
                    // add() returns false if another worker claimed it first. One atomic
                    // operation, so there is no window between the check and the act.
                    if (visited.add(next)) {
                        submit(pool, phaser, web, visited, next, depth + 1, maxDepth);
                    }
                }
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    /** A deliberately cyclic graph: a -> b -> c -> a. An unguarded crawler loops forever here. */
    static FakeWeb sampleWeb() {
        return new FakeWeb(Map.of(
                "/a", List.of("/b", "/c"),
                "/b", List.of("/c", "/d"),
                "/c", List.of("/a", "/e"),     // cycle back to /a
                "/d", List.of("/e", "/f"),
                "/e", List.of("/b"),           // cycle back to /b
                "/f", List.of("/g"),
                "/g", List.of("/a"),           // cycle back to /a
                "/orphan", List.of("/a")));    // unreachable from the seed
    }

    static void main() {
        int passed = 0, failed = 0;

        System.out.println("=== Concurrent crawl of a cyclic link graph ===\n");

        CrawlResult result = crawl(sampleWeb(), "/a", 4, 10);
        System.out.println("visited (" + result.visited().size() + "): "
                + result.visited().stream().sorted().toList());
        System.out.println("fetches: " + result.fetches() + "   elapsed: " + result.millis() + "ms");

        System.out.println("\n--- checks ---");

        // 1. Terminates at all. A crawler with no visited-set guard never returns on this graph.
        System.out.println("crawl terminated on a cyclic graph          : true");
        passed++;

        // 2. Every reachable page found; the orphan is correctly not reached.
        Set<String> expected = Set.of("/a", "/b", "/c", "/d", "/e", "/f", "/g");
        boolean c2 = result.visited().equals(expected);
        System.out.println("found exactly the reachable pages           : " + c2);
        if (c2) passed++; else failed++;

        // 3. THE correctness property: each page fetched exactly once despite 4 workers racing.
        boolean c3 = result.fetches() == expected.size();
        System.out.printf("each page fetched exactly once (%d fetches)  : %s%n", result.fetches(), c3);
        System.out.println("    ^ this is what ConcurrentHashMap.newKeySet().add() buys:");
        System.out.println("      check-and-claim in ONE atomic step, so two workers cannot");
        System.out.println("      both pass the 'not visited yet' test");
        if (c3) passed++; else failed++;

        // 4. Repeat the crawl many times — a race would show up intermittently, so once is
        //    not evidence. This is the standard way to test concurrent code.
        boolean stable = true;
        for (int i = 0; i < 30; i++) {
            CrawlResult r = crawl(sampleWeb(), "/a", 8, 10);
            if (!r.visited().equals(expected) || r.fetches() != expected.size()) {
                stable = false;
                break;
            }
        }
        System.out.println("stable across 30 runs with 8 workers        : " + stable);
        System.out.println("    ^ a concurrency test that runs once proves almost nothing");
        if (stable) passed++; else failed++;

        // 5. Depth limiting works.
        CrawlResult shallow = crawl(sampleWeb(), "/a", 4, 1);
        boolean c5 = shallow.visited().equals(Set.of("/a", "/b", "/c"));
        System.out.println("maxDepth=1 stops after the seed's links     : " + c5);
        if (c5) passed++; else failed++;

        // 6. Depth 0 fetches nothing at all.
        CrawlResult none = crawl(sampleWeb(), "/a", 4, 0);
        boolean c6 = none.visited().equals(Set.of("/a")) && none.fetches() == 0;
        System.out.println("maxDepth=0 visits only the seed             : " + c6);
        if (c6) passed++; else failed++;

        // 7. More workers should not change the result, only the wall-clock.
        CrawlResult wide = crawl(sampleWeb(), "/a", 16, 10);
        boolean c7 = wide.visited().equals(expected) && wide.fetches() == expected.size();
        System.out.println("result independent of worker count          : " + c7);
        if (c7) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: crawler is race-free, cycle-safe, depth-bounded, and terminates correctly."
                : "FAIL: concurrent crawler mismatch.");
    }
}
