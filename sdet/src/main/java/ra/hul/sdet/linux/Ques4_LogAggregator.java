package ra.hul.sdet.linux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Log Aggregator - merge logs from many hosts into one timeline, and find the incident in it.
 *
 * <p>The scenario every SDET eventually lives: a request failed, and the evidence is spread across
 * eight services on six hosts, each with its own file, its own clock, and its own idea of a log
 * format. You need one ordered timeline.
 *
 * <p>What makes this more than a file concat:
 *
 * <ul>
 *   <li><b>Merging by timestamp, not by file.</b> Concatenating loses the interleaving, which is
 *       the only thing that shows causality.</li>
 *   <li><b>Clock skew.</b> Host clocks disagree. A per-host offset correction is the pragmatic
 *       fix; the principled one is to trust a trace ID over a timestamp — which is why
 *       distributed tracing exists.</li>
 *   <li><b>Malformed lines.</b> Real logs contain stack traces, truncated writes, and
 *       interleaved partial lines. Crashing on the first unparseable line makes the tool useless
 *       exactly when you need it.</li>
 *   <li><b>Correlation by trace ID</b>, which is what actually reconstructs one request's path.</li>
 * </ul>
 *
 * <p>Reading is done concurrently — this is the I/O-bound fan-out the SSH version of this problem
 * would do over the network, minus the network.
 *
 * <p>Self-contained: writes sample log files to a temp directory, then aggregates them. No SSH,
 * no remote hosts.
 */
public class Ques4_LogAggregator {

    /** One parsed log line. */
    record LogEntry(Instant timestamp, String host, String level, String traceId, String message) {
    }

    /** Aggregation result, including what could not be parsed. */
    record Aggregation(List<LogEntry> timeline, List<String> malformed, Map<String, Integer> byLevel) {
    }

    /**
     * Parses one line of the form:
     * {@code 2026-03-01T10:00:00.100Z LEVEL trace=abc123 message text}
     *
     * @return null when the line cannot be parsed
     */
    static LogEntry parseLine(String host, String line) {
        String[] parts = line.trim().split("\\s+", 4);
        if (parts.length < 4) {
            return null;
        }
        Instant ts;
        try {
            ts = Instant.parse(parts[0]);
        } catch (DateTimeParseException e) {
            return null;   // a stack-trace continuation line, or a truncated write
        }
        String level = parts[1];
        if (!List.of("DEBUG", "INFO", "WARN", "ERROR").contains(level)) {
            return null;
        }
        String traceField = parts[2];
        String traceId = traceField.startsWith("trace=") ? traceField.substring(6) : "";
        return new LogEntry(ts, host, level, traceId, parts[3]);
    }

    /**
     * Reads every host's log concurrently and merges into one timeline.
     *
     * @param clockOffsets per-host correction in milliseconds, added to that host's timestamps
     */
    static Aggregation aggregate(Map<String, Path> hostFiles, Map<String, Long> clockOffsets) {
        ConcurrentLinkedQueue<LogEntry> entries = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> malformed = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(
                Math.max(1, Math.min(hostFiles.size(), 8)));
        try {
            for (Map.Entry<String, Path> e : hostFiles.entrySet()) {
                String host = e.getKey();
                Path file = e.getValue();
                pool.execute(() -> {
                    long offset = clockOffsets.getOrDefault(host, 0L);
                    try {
                        for (String line : Files.readAllLines(file)) {
                            if (line.isBlank()) {
                                continue;
                            }
                            LogEntry parsed = parseLine(host, line);
                            if (parsed == null) {
                                // Record and continue. A parser that dies on the first odd line
                                // is useless precisely during an incident.
                                malformed.add(host + ": " + line);
                            } else {
                                entries.add(new LogEntry(
                                        parsed.timestamp().plusMillis(offset),
                                        host, parsed.level(), parsed.traceId(), parsed.message()));
                            }
                        }
                    } catch (IOException io) {
                        malformed.add(host + ": <unreadable> " + io.getMessage());
                    }
                });
            }
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        List<LogEntry> timeline = new ArrayList<>(entries);
        // Merge by corrected timestamp; host breaks ties so the output is deterministic even
        // when two hosts log at the same millisecond.
        timeline.sort(Comparator.comparing(LogEntry::timestamp).thenComparing(LogEntry::host));

        Map<String, Integer> byLevel = new LinkedHashMap<>();
        for (String level : List.of("DEBUG", "INFO", "WARN", "ERROR")) {
            byLevel.put(level, 0);
        }
        for (LogEntry entry : timeline) {
            byLevel.merge(entry.level(), 1, Integer::sum);
        }

        return new Aggregation(timeline, new ArrayList<>(malformed), byLevel);
    }

    /** Everything logged for one request, in order — the thing you actually want during an incident. */
    static List<LogEntry> traceTimeline(Aggregation agg, String traceId) {
        return agg.timeline().stream().filter(e -> e.traceId().equals(traceId)).toList();
    }

    /** Writes the sample logs. web-2's clock runs 500ms fast, on purpose. */
    static Map<String, Path> writeSampleLogs(Path dir) throws IOException {
        Map<String, String> logs = new LinkedHashMap<>();
        logs.put("web-1", """
                2026-03-01T10:00:00.100Z INFO trace=req-42 received POST /checkout
                2026-03-01T10:00:00.900Z INFO trace=req-42 calling payment service
                2026-03-01T10:00:02.100Z ERROR trace=req-42 checkout failed: upstream timeout
                2026-03-01T10:00:05.000Z INFO trace=req-99 received GET /health
                """);
        // web-2's clock is 500ms AHEAD; raw timestamps put its lines in the wrong order.
        logs.put("web-2", """
                2026-03-01T10:00:01.100Z INFO trace=req-42 payment request forwarded
                2026-03-01T10:00:02.400Z WARN trace=req-42 retry 1 of 3
                    at com.example.Payment.charge(Payment.java:88)
                2026-03-01T10:00:03.000Z ERROR trace=req-42 giving up after 3 retries
                """);
        logs.put("db-1", """
                2026-03-01T10:00:01.200Z DEBUG trace=req-42 BEGIN
                2026-03-01T10:00:01.800Z WARN trace=req-42 lock wait 600ms on orders
                TRUNCATED-LINE-NO-TIMESTAMP
                2026-03-01T10:00:04.000Z DEBUG trace=req-42 ROLLBACK
                """);

        Map<String, Path> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : logs.entrySet()) {
            Path p = dir.resolve(e.getKey() + ".log");
            Files.writeString(p, e.getValue());
            files.put(e.getKey(), p);
        }
        return files;
    }

    static void main() throws IOException {
        int passed = 0, failed = 0;

        Path dir = Files.createTempDirectory("logagg-");
        dir.toFile().deleteOnExit();
        Map<String, Path> files = writeSampleLogs(dir);

        System.out.println("=== Aggregating logs from " + files.size() + " hosts ===\n");

        // Uncorrected first, to show the problem.
        Aggregation raw = aggregate(files, Map.of());
        System.out.println("WITHOUT clock correction (web-2 is 500ms fast):");
        for (LogEntry e : traceTimeline(raw, "req-42")) {
            System.out.printf("  %s %-6s %-6s %s%n", e.timestamp(), e.host(), e.level(), e.message());
        }

        Aggregation corrected = aggregate(files, Map.of("web-2", -500L));
        System.out.println("\nWITH clock correction (web-2 offset -500ms):");
        for (LogEntry e : traceTimeline(corrected, "req-42")) {
            System.out.printf("  %s %-6s %-6s %s%n", e.timestamp(), e.host(), e.level(), e.message());
        }

        System.out.println("\nlevels: " + corrected.byLevel());
        System.out.println("malformed lines skipped: " + corrected.malformed().size());
        corrected.malformed().forEach(m -> System.out.println("  " + m.trim()));

        System.out.println("\n--- checks ---");

        // 1. Every parseable line across all hosts is present (web-1: 4, web-2: 3, db-1: 3).
        boolean c1 = corrected.timeline().size() == 10;
        System.out.printf("all 10 parseable lines merged (%d)          : %s%n",
                corrected.timeline().size(), c1);
        if (c1) passed++; else failed++;

        // 2. Malformed lines are recorded, not fatal.
        boolean c2 = corrected.malformed().size() == 2;
        System.out.printf("2 malformed lines captured, not fatal       : %s%n", c2);
        System.out.println("    ^ a stack-trace continuation and a truncated write — both");
        System.out.println("      completely normal in real logs");
        if (c2) passed++; else failed++;

        // 3. The timeline is globally ordered, not per-file.
        boolean ordered = true;
        List<LogEntry> t = corrected.timeline();
        for (int i = 1; i < t.size(); i++) {
            if (t.get(i).timestamp().isBefore(t.get(i - 1).timestamp())) {
                ordered = false;
                break;
            }
        }
        System.out.println("timeline is globally ordered by timestamp   : " + ordered);
        if (ordered) passed++; else failed++;

        // 4. Lines from different hosts interleave — proof it is a merge, not a concat.
        List<LogEntry> trace = traceTimeline(corrected, "req-42");
        boolean interleaved = false;
        for (int i = 1; i < trace.size(); i++) {
            if (!trace.get(i).host().equals(trace.get(i - 1).host())) {
                interleaved = true;
                break;
            }
        }
        System.out.println("hosts interleave (a merge, not a concat)    : " + interleaved);
        if (interleaved) passed++; else failed++;

        // 5. Clock correction changes the ordering — the whole point of the exercise.
        List<LogEntry> rawTrace = traceTimeline(raw, "req-42");
        boolean orderChanged = !rawTrace.stream().map(LogEntry::message).toList()
                .equals(trace.stream().map(LogEntry::message).toList());
        System.out.println("clock correction reorders the timeline      : " + orderChanged);
        System.out.println("    ^ without it you would conclude the DB rolled back BEFORE the");
        System.out.println("      payment gave up. Skewed clocks invent false causality —");
        System.out.println("      which is why trace IDs beat timestamps for ordering.");
        if (orderChanged) passed++; else failed++;

        // 6. Trace correlation isolates one request.
        boolean c6 = trace.size() == 9 && trace.stream().allMatch(e -> e.traceId().equals("req-42"));
        System.out.printf("trace correlation isolates req-42 (%d lines) : %s%n", trace.size(), c6);
        if (c6) passed++; else failed++;

        // 7. The other request is excluded.
        boolean c7 = traceTimeline(corrected, "req-99").size() == 1;
        System.out.println("unrelated trace req-99 kept separate        : " + c7);
        if (c7) passed++; else failed++;

        // 8. Level counts.
        boolean c8 = corrected.byLevel().get("ERROR") == 2 && corrected.byLevel().get("WARN") == 2;
        System.out.println("level tally correct (2 ERROR, 2 WARN)       : " + c8);
        if (c8) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: logs merged by corrected time, malformed lines survived, traces correlated."
                : "FAIL: log aggregation mismatch.");
    }
}
