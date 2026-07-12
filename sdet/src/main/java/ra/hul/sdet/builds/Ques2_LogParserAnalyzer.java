package ra.hul.sdet.builds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Log Parser and Analyzer - Parse app logs `[TIMESTAMP] [LEVEL] [CLASS] - Message`, filter, aggregate,
 * and flag anomalies.
 * Common SDET question (machine-coding round): "Parse a log file; filter by date range / level / keyword;
 * count by level; report errors-per-hour; detect a sudden spike in ERROR count."
 *
 * Self-contained: builds a sample log as a temp file in main(), analyzes it, prints results, cleans up.
 */
public class Ques2_LogParserAnalyzer {

    private static final Pattern LINE = Pattern.compile(
            "^\\[([^\\]]+)]\\s+\\[(\\w+)]\\s+\\[([^\\]]+)]\\s+-\\s+(.*)$");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** One parsed log record. */
    public record LogEntry(LocalDateTime time, String level, String clazz, String message) {}

    /** Filter criteria; any field may be null to mean "no constraint". */
    public record Filter(LocalDateTime from, LocalDateTime to, String level, String keyword) {
        boolean matches(LogEntry e) {
            if (from != null && e.time().isBefore(from)) return false;
            if (to != null && e.time().isAfter(to)) return false;
            if (level != null && !level.equalsIgnoreCase(e.level())) return false;
            if (keyword != null && !e.message().toLowerCase().contains(keyword.toLowerCase())) return false;
            return true;
        }
    }

    public static List<LogEntry> parse(Path logFile) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        try (var lines = Files.lines(logFile)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank()) continue;
                Matcher m = LINE.matcher(line);
                if (!m.matches()) continue;
                entries.add(new LogEntry(
                        LocalDateTime.parse(m.group(1), TS), m.group(2), m.group(3), m.group(4)));
            }
        }
        return entries;
    }

    public static List<LogEntry> filter(List<LogEntry> entries, Filter f) {
        return entries.stream().filter(f::matches).toList();
    }

    /** Count entries by level. */
    public static Map<String, Long> countByLevel(List<LogEntry> entries) {
        Map<String, Long> counts = new TreeMap<>();
        for (LogEntry e : entries) counts.merge(e.level(), 1L, Long::sum);
        return counts;
    }

    /** ERROR count per hour bucket (yyyy-MM-dd HH), sorted chronologically. */
    public static Map<String, Long> errorsPerHour(List<LogEntry> entries) {
        Map<String, Long> perHour = new TreeMap<>();
        DateTimeFormatter hour = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
        for (LogEntry e : entries) {
            if (e.level().equalsIgnoreCase("ERROR"))
                perHour.merge(e.time().format(hour), 1L, Long::sum);
        }
        return perHour;
    }

    /** Anomaly detection: an hour whose ERROR count exceeds mean + factor*(previous hour) style spike.
     * Simple rule: flag any hour where errors >= threshold AND >= 2x the average of the other hours. */
    public static List<String> detectSpikes(Map<String, Long> errorsPerHour, long threshold) {
        List<String> spikes = new ArrayList<>();
        if (errorsPerHour.isEmpty()) return spikes;
        long total = errorsPerHour.values().stream().mapToLong(Long::longValue).sum();
        for (var e : errorsPerHour.entrySet()) {
            long others = errorsPerHour.size() > 1 ? (total - e.getValue()) / (errorsPerHour.size() - 1) : 0;
            if (e.getValue() >= threshold && e.getValue() >= 2 * Math.max(1, others))
                spikes.add(e.getKey() + " (" + e.getValue() + " errors)");
        }
        return spikes;
    }

    static void main() throws IOException {
        Path log = Files.createTempFile("app", ".log");
        try {
            String sample = """
                    [2026-07-12 09:00:01] [INFO] [OrderSvc] - Service started
                    [2026-07-12 09:15:00] [WARN] [OrderSvc] - Cache miss for key=42
                    [2026-07-12 09:30:00] [ERROR] [PaymentSvc] - Timeout calling gateway
                    [2026-07-12 10:01:00] [ERROR] [PaymentSvc] - Timeout calling gateway
                    [2026-07-12 10:05:00] [ERROR] [PaymentSvc] - Connection refused
                    [2026-07-12 10:07:00] [ERROR] [PaymentSvc] - Connection refused
                    [2026-07-12 10:09:00] [ERROR] [PaymentSvc] - Timeout calling gateway
                    [2026-07-12 10:59:00] [INFO] [OrderSvc] - Order 100 placed
                    """;
            Files.writeString(log, sample);

            List<LogEntry> all = parse(log);
            System.out.println("=== Log Analyzer ===");
            System.out.println("Parsed entries : " + all.size());

            Map<String, Long> byLevel = countByLevel(all);
            System.out.println("Count by level : " + byLevel);

            Map<String, Long> errHour = errorsPerHour(all);
            System.out.println("Errors per hour: " + errHour);

            List<String> spikes = detectSpikes(errHour, 3);
            System.out.println("ERROR spikes   : " + spikes);

            Filter f = new Filter(null, null, "ERROR", "timeout");
            List<LogEntry> filtered = filter(all, f);
            System.out.println("Filter ERROR+'timeout' -> " + filtered.size() + " matches");

            boolean ok = all.size() == 8
                    && byLevel.get("ERROR") == 5
                    && errHour.get("2026-07-12 10") == 4
                    && spikes.size() == 1 && spikes.getFirst().startsWith("2026-07-12 10")
                    && filtered.size() == 3;
            System.out.println(ok ? "PASSED: log parsing, aggregation, filtering, and spike detection match."
                    : "FAILED: analyzer mismatch.");
        } finally {
            Files.deleteIfExists(log);
        }
    }
}
