package ra.hul.sdet.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Rate Limiting Retry - Retry a request with exponential backoff on non-2xx responses,
 * simulating a 429 Too Many Requests via httpbin.
 * Common SDET question: "When an API returns 429, retry with exponential backoff (1s, 2s, 4s...),
 * respect a max attempt count and the Retry-After header, and log each attempt."
 *
 * Target: https://httpbin.org (free, no auth). NEEDS NETWORK.
 * /status/429 always returns 429 (to demonstrate backoff + attempt exhaustion);
 * /status/200 returns 200 (to demonstrate a successful eventual outcome).
 */
public class Ques5_RateLimitingRetry {

    /** Result of a retry run: which status won and how many attempts it took. */
    record RetryResult(int status, int attempts, boolean succeeded) {}

    /**
     * Calls {@code path} up to {@code maxAttempts} times, backing off exponentially
     * (baseMillis * 2^(attempt-1)) whenever the status is not 2xx. Honors a Retry-After
     * header when present. Backoff is capped small here to keep the demo fast.
     */
    static RetryResult callWithBackoff(String path, int maxAttempts, long baseMillis) {
        int status = -1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Response r = given().when().get(path);
            status = r.statusCode();
            System.out.printf("  attempt %d/%d -> %d%n", attempt, maxAttempts, status);

            if (status >= 200 && status < 300) {
                return new RetryResult(status, attempt, true);
            }
            if (attempt == maxAttempts) break; // exhausted - do not sleep again

            long backoff = baseMillis * (1L << (attempt - 1)); // 1x, 2x, 4x, ...
            String retryAfter = r.getHeader("Retry-After");
            if (retryAfter != null) {
                try { backoff = Math.max(backoff, Long.parseLong(retryAfter.trim()) * 1000L); }
                catch (NumberFormatException ignore) { /* keep computed backoff */ }
            }
            // Cap so the demo stays quick regardless of computed/header value.
            backoff = Math.min(backoff, 400L);
            System.out.printf("    non-2xx, backing off %d ms%n", backoff);
            try { Thread.sleep(backoff); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return new RetryResult(status, maxAttempts, false);
    }

    static void main() {
        RestAssured.baseURI = "https://httpbin.org";

        System.out.println("=== Scenario A: persistent 429 exhausts all attempts ===");
        RetryResult a = callWithBackoff("/status/429", 3, 100L);
        boolean aOk = !a.succeeded() && a.status() == 429 && a.attempts() == 3;
        System.out.println(aOk
                ? "PASSED: exhausted 3 attempts on persistent 429 with exponential backoff."
                : "FAILED: unexpected retry outcome for 429 -> " + a);

        System.out.println("=== Scenario B: 200 succeeds on first attempt (no backoff) ===");
        RetryResult b = callWithBackoff("/status/200", 3, 100L);
        boolean bOk = b.succeeded() && b.status() == 200 && b.attempts() == 1;
        System.out.println(bOk
                ? "PASSED: 200 succeeded immediately, no retries needed."
                : "FAILED: unexpected retry outcome for 200 -> " + b);
    }
}
