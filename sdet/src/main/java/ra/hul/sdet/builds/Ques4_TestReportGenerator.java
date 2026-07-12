package ra.hul.sdet.builds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test Report Generator - Turn pass/fail/skip test results into a self-contained HTML report.
 * Common SDET question (machine-coding round): "Given test results, generate an HTML report with a
 * summary (total/pass/fail/skip/duration) and a per-test table; write it to a file."
 *
 * Self-contained: builds sample results in main(), renders HTML (inline CSS + SVG donut, no external
 * assets), writes it to a temp file, verifies the content, cleans up.
 */
public class Ques4_TestReportGenerator {

    public enum Status { PASS, FAIL, SKIP }

    /** One test method result. */
    public record TestResult(String testClass, String method, Status status, long durationMs, String failure) {}

    /** Aggregate counts. */
    public record Summary(int total, int pass, int fail, int skip, long totalMs) {
        static Summary of(List<TestResult> results) {
            int p = 0, f = 0, s = 0; long ms = 0;
            for (TestResult r : results) {
                switch (r.status()) { case PASS -> p++; case FAIL -> f++; case SKIP -> s++; }
                ms += r.durationMs();
            }
            return new Summary(results.size(), p, f, s, ms);
        }
    }

    /** Render a complete, standalone HTML document as a String. */
    public static String renderHtml(List<TestResult> results) {
        Summary s = Summary.of(results);
        double passPct = s.total() == 0 ? 0 : (100.0 * s.pass() / s.total());

        StringBuilder rows = new StringBuilder();
        for (TestResult r : results) {
            String color = switch (r.status()) {
                case PASS -> "#2e7d32"; case FAIL -> "#c62828"; case SKIP -> "#f9a825";
            };
            rows.append("""
                    <tr>
                      <td>%s</td><td>%s</td>
                      <td style="color:%s;font-weight:bold">%s</td>
                      <td>%d ms</td><td><pre>%s</pre></td>
                    </tr>
                    """.formatted(esc(r.testClass()), esc(r.method()), color, r.status(),
                    r.durationMs(), esc(r.failure() == null ? "" : r.failure())));
        }

        // Simple SVG donut for pass/fail/skip ratio (no external chart lib).
        double passFrac = s.total() == 0 ? 0 : (double) s.pass() / s.total();
        double failFrac = s.total() == 0 ? 0 : (double) s.fail() / s.total();
        double circ = 2 * Math.PI * 40;
        String donut = """
                <svg width="120" height="120" viewBox="0 0 120 120">
                  <circle cx="60" cy="60" r="40" fill="none" stroke="#f9a825" stroke-width="16"/>
                  <circle cx="60" cy="60" r="40" fill="none" stroke="#c62828" stroke-width="16"
                          stroke-dasharray="%.2f %.2f" transform="rotate(-90 60 60)"/>
                  <circle cx="60" cy="60" r="40" fill="none" stroke="#2e7d32" stroke-width="16"
                          stroke-dasharray="%.2f %.2f"
                          stroke-dashoffset="%.2f" transform="rotate(-90 60 60)"/>
                </svg>""".formatted(
                (failFrac + passFrac) * circ, circ,
                passFrac * circ, circ, 0.0);

        return """
                <!doctype html><html><head><meta charset="utf-8"><title>Test Report</title>
                <style>
                  body{font-family:system-ui,Arial,sans-serif;margin:24px;color:#222}
                  .cards{display:flex;gap:16px;align-items:center;margin:16px 0}
                  .card{padding:12px 18px;border-radius:8px;background:#f4f4f4;min-width:70px;text-align:center}
                  table{border-collapse:collapse;width:100%%;margin-top:16px}
                  th,td{border:1px solid #ddd;padding:6px 10px;text-align:left;font-size:14px}
                  th{background:#fafafa} pre{margin:0;white-space:pre-wrap}
                </style></head><body>
                <h1>Test Execution Report</h1>
                <div class="cards">
                  %s
                  <div class="card"><b>%d</b><br>Total</div>
                  <div class="card" style="background:#e8f5e9"><b>%d</b><br>Pass</div>
                  <div class="card" style="background:#ffebee"><b>%d</b><br>Fail</div>
                  <div class="card" style="background:#fffde7"><b>%d</b><br>Skip</div>
                  <div class="card"><b>%.1f%%</b><br>Pass rate</div>
                  <div class="card"><b>%d ms</b><br>Duration</div>
                </div>
                <table>
                  <thead><tr><th>Class</th><th>Method</th><th>Status</th><th>Time</th><th>Failure</th></tr></thead>
                  <tbody>%s</tbody>
                </table>
                </body></html>
                """.formatted(donut, s.total(), s.pass(), s.fail(), s.skip(), passPct, s.totalMs(), rows);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void main() throws IOException {
        List<TestResult> results = List.of(
                new TestResult("LoginTest", "validCredentials", Status.PASS, 120, null),
                new TestResult("LoginTest", "invalidPassword", Status.PASS, 95, null),
                new TestResult("CheckoutTest", "applyCoupon", Status.FAIL, 210,
                        "AssertionError: expected 90 but was 100"),
                new TestResult("CheckoutTest", "guestCheckout", Status.SKIP, 0, "env not ready"));

        String html = renderHtml(results);
        Path report = Files.createTempFile("test-report", ".html");
        try {
            Files.writeString(report, html);
            System.out.println("=== Test Report Generator ===");
            System.out.println("Wrote report: " + report + " (" + Files.size(report) + " bytes)");

            Summary s = Summary.of(results);
            System.out.printf("Summary: total=%d pass=%d fail=%d skip=%d duration=%dms%n",
                    s.total(), s.pass(), s.fail(), s.skip(), s.totalMs());

            boolean ok = s.total() == 4 && s.pass() == 2 && s.fail() == 1 && s.skip() == 1
                    && html.contains("<title>Test Report</title>")
                    && html.contains("expected 90 but was 100")   // failure surfaced (escaped)
                    && html.contains("<svg")                        // chart embedded
                    && !html.contains("http://") && !html.contains("https://"); // self-contained
            System.out.println(ok ? "PASSED: HTML report is self-contained with correct summary."
                    : "FAILED: report content mismatch.");
        } finally {
            Files.deleteIfExists(report);
        }
    }
}
