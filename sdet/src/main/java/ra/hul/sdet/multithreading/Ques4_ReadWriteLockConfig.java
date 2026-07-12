package ra.hul.sdet.multithreading;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Read-Write Lock Config - Shared config store: concurrent readers, exclusive writer, via ReentrantReadWriteLock.
 * Common SDET question: "Show that readers run concurrently but a writer blocks everyone."
 *
 * Self-contained (no network). Runs deterministically to completion.
 */
public class Ques4_ReadWriteLockConfig {

    /** Config store guarded by a ReentrantReadWriteLock: many readers share, one writer excludes all. */
    static final class ConfigStore {
        private final Map<String, String> config = new HashMap<>();
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        // Tracks how many readers are inside the read section at once, to prove concurrency.
        private final AtomicInteger activeReaders = new AtomicInteger(0);
        private volatile int maxConcurrentReaders = 0;

        String get(String key) {
            lock.readLock().lock();
            try {
                int now = activeReaders.incrementAndGet();
                maxConcurrentReaders = Math.max(maxConcurrentReaders, now);
                try {
                    Thread.sleep(20); // hold the read lock briefly so readers overlap
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return config.get(key);
            } finally {
                activeReaders.decrementAndGet();
                lock.readLock().unlock();
            }
        }

        void put(String key, String value) {
            lock.writeLock().lock();
            try {
                // A writer must observe zero readers, proving exclusivity.
                if (activeReaders.get() != 0) {
                    throw new IllegalStateException("writer saw active readers - lock is broken");
                }
                config.put(key, value);
            } finally {
                lock.writeLock().unlock();
            }
        }

        int maxConcurrentReaders() { return maxConcurrentReaders; }
    }

    static void main() throws InterruptedException {
        ConfigStore store = new ConfigStore();
        store.put("env", "staging");

        int readerCount = 6;
        CountDownLatch done = new CountDownLatch(readerCount + 1);
        final boolean[] writerOk = { true };

        // Launch many readers concurrently.
        for (int i = 0; i < readerCount; i++) {
            new Thread(() -> {
                try {
                    for (int r = 0; r < 3; r++) {
                        store.get("env");
                    }
                } finally {
                    done.countDown();
                }
            }, "reader-" + i).start();
        }

        // One writer running alongside; its invariant check throws if exclusivity is violated.
        new Thread(() -> {
            try {
                for (int w = 0; w < 5; w++) {
                    store.put("env", "prod-" + w);
                    Thread.sleep(10);
                }
            } catch (IllegalStateException e) {
                writerOk[0] = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "writer").start();

        done.await();

        int maxReaders = store.maxConcurrentReaders();
        System.out.printf("Max concurrent readers observed: %d; writer exclusivity held: %b%n", maxReaders, writerOk[0]);

        if (maxReaders > 1 && writerOk[0]) {
            System.out.println("PASSED: readers ran concurrently while the writer had exclusive access.");
        } else {
            System.out.println("FAILED: expected concurrent readers and exclusive writer.");
        }
    }
}
