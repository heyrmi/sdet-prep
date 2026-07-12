package ra.hul.sdet.designpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * Observer Pattern for Test Events - a publisher notifies many listeners on test lifecycle events.
 * Common SDET question: "Fan out test started/passed/failed to Logger, ScreenshotCapture, SlackNotifier."
 *
 * Self-contained: listeners just record/print. Mirrors TestNG's ITestListener where each callback
 * (onTestStart/onTestSuccess/onTestFailure) is broadcast to every registered listener.
 * Run main() — no browser/network needed.
 */
public class Ques6_ObserverTestEvents {

    enum Status { STARTED, PASSED, FAILED, SKIPPED }
    record TestEvent(String testName, Status status, String detail) {}

    /** Observer contract (like TestNG ITestListener). */
    interface TestEventListener { void onEvent(TestEvent e); }

    /** Logs every event. */
    static final class LoggerListener implements TestEventListener {
        public void onEvent(TestEvent e) {
            System.out.println("  [LOG] " + e.testName() + " -> " + e.status()
                    + (e.detail().isEmpty() ? "" : " (" + e.detail() + ")"));
        }
    }

    /** Grabs a screenshot only on failure (stub: just counts). */
    static final class ScreenshotStubListener implements TestEventListener {
        int captured = 0;
        public void onEvent(TestEvent e) {
            if (e.status() == Status.FAILED) {
                captured++;
                System.out.println("  [SHOT] saved screenshot for failed test: " + e.testName()); // real: driver.getScreenshotAs(...)
            }
        }
    }

    /** Sends a notification on failure (stub: no real Slack call). */
    static final class NotifierListener implements TestEventListener {
        int notifications = 0;
        public void onEvent(TestEvent e) {
            if (e.status() == Status.FAILED) {
                notifications++;
                System.out.println("  [NOTIFY] Slack: '" + e.testName() + "' FAILED - " + e.detail());
            }
        }
    }

    /** Subject/publisher: registers listeners and broadcasts each event to all of them. */
    static final class TestEventPublisher {
        private final List<TestEventListener> listeners = new ArrayList<>();
        void register(TestEventListener l) { listeners.add(l); }
        void publish(TestEvent e) { for (TestEventListener l : listeners) l.onEvent(e); }
    }

    static void main() {
        TestEventPublisher publisher = new TestEventPublisher();
        LoggerListener logger = new LoggerListener();
        ScreenshotStubListener screenshots = new ScreenshotStubListener();
        NotifierListener notifier = new NotifierListener();
        publisher.register(logger);
        publisher.register(screenshots);
        publisher.register(notifier);

        // Simulate a test run.
        publisher.publish(new TestEvent("loginTest", Status.STARTED, ""));
        publisher.publish(new TestEvent("loginTest", Status.PASSED, ""));
        publisher.publish(new TestEvent("checkoutTest", Status.STARTED, ""));
        publisher.publish(new TestEvent("checkoutTest", Status.FAILED, "payment timeout"));
        publisher.publish(new TestEvent("searchTest", Status.SKIPPED, "dependency down"));
        publisher.publish(new TestEvent("cartTest", Status.FAILED, "element not found"));

        boolean ok = screenshots.captured == 2 && notifier.notifications == 2;
        System.out.println("Screenshots captured: " + screenshots.captured + ", notifications sent: " + notifier.notifications);
        System.out.println(ok ? "PASSED: all observers were notified; failure-only observers fired twice."
                              : "FAILED: observer fan-out unexpected.");
    }
}
