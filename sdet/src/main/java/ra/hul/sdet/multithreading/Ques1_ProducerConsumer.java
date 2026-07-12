package ra.hul.sdet.multithreading;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producer-Consumer - Produce test URLs on one thread, consume/test them on another via a BlockingQueue.
 * Common SDET question: "Implement producer-consumer with a BlockingQueue and graceful shutdown (poison pill)."
 *
 * Self-contained (no network). Runs deterministically to completion.
 */
public class Ques1_ProducerConsumer {

    /** Sentinel that tells a consumer to stop. */
    private static final String POISON_PILL = "__STOP__";

    /** Produces the given work items onto the queue, then one poison pill per consumer for graceful shutdown. */
    static Thread producer(BlockingQueue<String> queue, List<String> work, int consumers) {
        Thread t = new Thread(() -> {
            try {
                for (String url : work) {
                    queue.put(url); // blocks if the queue is full
                }
                for (int i = 0; i < consumers; i++) {
                    queue.put(POISON_PILL); // one pill per consumer guarantees each one exits
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");
        t.start();
        return t;
    }

    /** Consumes URLs until it receives a poison pill; increments the shared processed counter for each real item. */
    static Thread consumer(BlockingQueue<String> queue, AtomicInteger processed, int id) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    String url = queue.take(); // blocks until an item is available
                    if (POISON_PILL.equals(url)) {
                        break; // graceful shutdown
                    }
                    // "test" the URL (simulated work)
                    processed.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer-" + id);
        t.start();
        return t;
    }

    static void main() throws InterruptedException {
        int consumerCount = 3;
        int itemCount = 30;
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(8); // bounded to exercise back-pressure
        AtomicInteger processed = new AtomicInteger(0);

        List<String> work = new java.util.ArrayList<>();
        for (int i = 1; i <= itemCount; i++) {
            work.add("https://example.com/page/" + i);
        }

        Thread prod = producer(queue, work, consumerCount);
        List<Thread> consumers = new java.util.ArrayList<>();
        for (int i = 1; i <= consumerCount; i++) {
            consumers.add(consumer(queue, processed, i));
        }

        prod.join();
        for (Thread c : consumers) {
            c.join();
        }

        int result = processed.get();
        System.out.printf("Produced %d items, consumed %d across %d consumers.%n", itemCount, result, consumerCount);
        if (result == itemCount && queue.isEmpty()) {
            System.out.println("PASSED: all items consumed exactly once and all consumers shut down gracefully.");
        } else {
            System.out.println("FAILED: expected " + itemCount + " consumed with an empty queue, got " + result);
        }
    }
}
