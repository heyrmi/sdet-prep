package ra.hul.sdet.fileops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Log Parser - Parse an Apache/Nginx access log and produce a summary report.
 * Common SDET question: "Parse a server access log: total requests, unique IPs, top endpoints, 4xx/5xx counts."
 *
 * Self-contained: builds a sample access log as a temp file in main(), parses it, prints results, cleans up.
 */
public class Ques2_LogParser {

    // Combined Log Format: IP - - [timestamp] "METHOD /path HTTP/1.1" status bytes
    private static final Pattern LINE = Pattern.compile(
            "^(\\S+) \\S+ \\S+ \\[([^\\]]+)] \"(\\S+) (\\S+) [^\"]*\" (\\d{3}) (\\S+)");

    /** Immutable summary of a parsed access log. */
    public record LogSummary(long totalRequests,
                             long malformedLines,
                             Map<String, Long> ipCounts,
                             Map<String, Long> endpointCounts,
                             long count4xx,
                             long count5xx) {
        long uniqueIps() { return ipCounts.size(); }
    }

    public static LogSummary parse(Path logFile) throws IOException {
        Map<String, Long> ipCounts = new HashMap<>();
        Map<String, Long> endpointCounts = new HashMap<>();
        long total = 0, malformed = 0, c4xx = 0, c5xx = 0;

        try (var lines = Files.lines(logFile)) {
            for (String line : (Iterable<String>) lines::iterator) {
                if (line.isBlank()) continue;
                Matcher m = LINE.matcher(line);
                if (!m.find()) { malformed++; continue; }
                total++;
                String ip = m.group(1);
                String endpoint = m.group(4);
                int status = Integer.parseInt(m.group(5));
                ipCounts.merge(ip, 1L, Long::sum);
                endpointCounts.merge(endpoint, 1L, Long::sum);
                if (status >= 400 && status < 500) c4xx++;
                else if (status >= 500 && status < 600) c5xx++;
            }
        }
        return new LogSummary(total, malformed, ipCounts, endpointCounts, c4xx, c5xx);
    }

    /** Top-N entries of a count map, ranked descending by count. */
    public static List<Map.Entry<String, Long>> topN(Map<String, Long> counts, int n) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(counts.entrySet());
        list.sort(Comparator.comparingLong((Map.Entry<String, Long> e) -> e.getValue()).reversed()
                .thenComparing(Map.Entry::getKey));
        return list.subList(0, Math.min(n, list.size()));
    }

    static void main() throws IOException {
        Path log = Files.createTempFile("access", ".log");
        try {
            String sample = """
                    10.0.0.1 - - [12/Jul/2026:10:00:01 +0000] "GET /home HTTP/1.1" 200 512
                    10.0.0.2 - - [12/Jul/2026:10:00:02 +0000] "GET /home HTTP/1.1" 200 512
                    10.0.0.1 - - [12/Jul/2026:10:00:03 +0000] "GET /api/users HTTP/1.1" 404 128
                    10.0.0.3 - - [12/Jul/2026:10:00:04 +0000] "POST /api/login HTTP/1.1" 500 64
                    10.0.0.1 - - [12/Jul/2026:10:00:05 +0000] "GET /home HTTP/1.1" 200 512
                    this is a malformed line that should be skipped
                    10.0.0.2 - - [12/Jul/2026:10:00:06 +0000] "GET /api/users HTTP/1.1" 403 128
                    """;
            Files.writeString(log, sample);

            LogSummary s = parse(log);

            System.out.println("=== Log Parser Report ===");
            System.out.println("Total requests : " + s.totalRequests());
            System.out.println("Malformed lines: " + s.malformedLines());
            System.out.println("Unique IPs     : " + s.uniqueIps());
            System.out.println("4xx count      : " + s.count4xx());
            System.out.println("5xx count      : " + s.count5xx());
            System.out.println("Top endpoints  : " + topN(s.endpointCounts(), 3));
            System.out.println("Top IPs        : " + topN(s.ipCounts(), 3));

            boolean ok = s.totalRequests() == 6
                    && s.malformedLines() == 1
                    && s.uniqueIps() == 3
                    && s.count4xx() == 2
                    && s.count5xx() == 1
                    && topN(s.endpointCounts(), 1).getFirst().getKey().equals("/home");
            System.out.println(ok ? "PASSED: log summary matches expected values."
                    : "FAILED: log summary mismatch.");
        } finally {
            Files.deleteIfExists(log);
        }
    }
}
