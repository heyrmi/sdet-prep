package ra.hul.sdet.designpatterns;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Singleton WebDriver Manager - thread-safe, one driver PER thread via ThreadLocal, lazy init.
 * Common SDET question: "Give each parallel test thread its own WebDriver while sharing config."
 *
 * Self-contained: {@code Driver} is a tiny stub instead of org.openqa.selenium.WebDriver.
 * Real usage: get() lazily builds a ChromeDriver; @AfterMethod calls quit() to clean up the ThreadLocal.
 * Run main() — spins up two threads and proves they get isolated instances.
 */
public class Ques2_SingletonWebDriverManager {

    interface Driver { String id(); void quit(); }

    static class StubDriver implements Driver {
        private final String id = "driver-" + System.nanoTime();
        public String id() { return id; }
        public void quit() { /* real: browser.quit() */ }
    }

    /** The Singleton manager: static ThreadLocal holds at most one Driver per thread. */
    static final class WebDriverManager {
        private WebDriverManager() {}                       // no instances
        private static final ThreadLocal<Driver> TL = ThreadLocal.withInitial(() -> {
            Driver d = new StubDriver();                    // lazy: created on first get() in each thread
            System.out.println("  [" + Thread.currentThread().getName() + "] created " + d.id());
            return d;
        });
        static Driver get() { return TL.get(); }
        static void quit() {                                // call in @AfterMethod / @AfterSuite
            Driver d = TL.get();
            if (d != null) { d.quit(); TL.remove(); }        // remove() prevents thread-pool leakage
        }
    }

    static void main() throws Exception {
        // Same thread -> same instance (true singleton per thread).
        String a = WebDriverManager.get().id();
        String b = WebDriverManager.get().id();
        boolean sameWithinThread = a.equals(b);
        System.out.println("Same thread, two get() calls equal? " + sameWithinThread);
        WebDriverManager.quit();

        // Two threads -> two distinct instances.
        var ids = new ConcurrentHashMap<String, String>();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Runnable task = () -> {
            String id = WebDriverManager.get().id();
            ids.put(Thread.currentThread().getName(), id);
            WebDriverManager.quit();
        };
        pool.submit(task); pool.submit(task);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        boolean isolatedAcrossThreads = ids.size() == 2
                && ids.values().stream().distinct().count() == 2;
        System.out.println("Per-thread instances: " + ids);
        System.out.println((sameWithinThread && isolatedAcrossThreads)
                ? "PASSED: singleton-per-thread; threads got isolated drivers."
                : "FAILED: driver isolation broken.");
    }
}
