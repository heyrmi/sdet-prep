package ra.hul.sdet.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Log Pattern Extractor - Parse log lines into structured fields using named regex groups.
 * Common SDET question: "Extract timestamp, level, thread, class and message from mixed log formats".
 *
 * Self-contained: main() self-verifies with PASS/FAIL. Handles two formats (Log4j-style and a custom bracketed one).
 */
public class Ques2_LogPatternExtractor {

    /** Structured log record built from a matched line. */
    public record LogEntry(String timestamp, String level, String thread, String clazz, String message) {}

    // Log4j-style: 2023-01-15 10:30:45,123 [main] INFO  com.example.Service - Started
    private static final Pattern LOG4J = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})\\s+"
                    + "\\[(?<thread>[^\\]]+)\\]\\s+"
                    + "(?<level>TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+"
                    + "(?<clazz>[\\w.$]+)\\s+-\\s+"
                    + "(?<message>.*)$");

    // Custom bracketed: [2023-01-15T10:30:45] [ERROR] [PaymentWorker] OrderService: Timeout calling gateway
    private static final Pattern CUSTOM = Pattern.compile(
            "^\\[(?<timestamp>[^\\]]+)\\]\\s+"
                    + "\\[(?<level>TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\]\\s+"
                    + "\\[(?<thread>[^\\]]+)\\]\\s+"
                    + "(?<clazz>[\\w.$]+):\\s+"
                    + "(?<message>.*)$");

    /** Returns a LogEntry if any known format matches, else null. */
    public static LogEntry parse(String line) {
        for (Pattern p : List.of(LOG4J, CUSTOM)) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                return new LogEntry(m.group("timestamp"), m.group("level"),
                        m.group("thread"), m.group("clazz"), m.group("message"));
            }
        }
        return null;
    }

    static void main() {
        List<String> lines = new ArrayList<>(List.of(
                "2023-01-15 10:30:45,123 [main] INFO  com.example.Service - Application started",
                "[2023-01-15T10:31:02] [ERROR] [PaymentWorker] OrderService: Timeout calling gateway",
                "this line is not a log entry"
        ));

        LogEntry e1 = parse(lines.get(0));
        LogEntry e2 = parse(lines.get(1));
        LogEntry e3 = parse(lines.get(2));

        System.out.println("Entry 1: " + e1);
        System.out.println("Entry 2: " + e2);
        System.out.println("Entry 3: " + e3);

        boolean pass =
                e1 != null && e1.level().equals("INFO") && e1.thread().equals("main")
                        && e1.clazz().equals("com.example.Service")
                        && e1.message().equals("Application started")
                        && e2 != null && e2.level().equals("ERROR") && e2.thread().equals("PaymentWorker")
                        && e2.clazz().equals("OrderService")
                        && e2.message().equals("Timeout calling gateway")
                        && e3 == null;

        System.out.println(pass ? "PASS: both formats parsed and non-log line rejected."
                : "FAIL: log extraction mismatch.");
    }
}
