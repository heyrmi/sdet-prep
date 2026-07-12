package ra.hul.sdet.fileops;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Tail Follow - Implement `tail -f`: print the last N lines, then follow appended data.
 * Common SDET question: "Implement tail -f in Java using RandomAccessFile seek and length polling."
 *
 * Self-contained and BOUNDED: main() prints the last N lines, then follows for a fixed number of
 * poll cycles while a background thread appends lines, then stops so the program terminates.
 */
public class Ques7_TailFollow {

    /** Read the last {@code n} lines of a file by seeking backward from the end. */
    public static List<String> lastNLines(Path file, int n) throws IOException {
        Deque<String> lines = new ArrayDeque<>();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long pointer = raf.length() - 1;
            StringBuilder sb = new StringBuilder();
            while (pointer >= 0 && lines.size() < n) {
                raf.seek(pointer);
                int b = raf.read();
                if (b == '\n') {
                    if (sb.length() > 0 || pointer != raf.length() - 1) {
                        lines.addFirst(sb.reverse().toString());
                        sb.setLength(0);
                        if (lines.size() == n) break;
                    }
                } else if (b != '\r') {
                    sb.append((char) b);
                }
                pointer--;
            }
            if (pointer < 0 && sb.length() > 0 && lines.size() < n) {
                lines.addFirst(sb.reverse().toString());
            }
        }
        return new ArrayList<>(lines);
    }

    /**
     * Follow the file from {@code startOffset}: poll length up to {@code maxCycles} times,
     * printing any newly appended complete lines. Returns the final read offset.
     */
    public static long follow(Path file, long startOffset, int maxCycles, long pollMillis)
            throws IOException, InterruptedException {
        long offset = startOffset;
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            for (int cycle = 0; cycle < maxCycles; cycle++) {
                long length = raf.length();
                if (length > offset) {
                    raf.seek(offset);
                    String line;
                    while ((line = raf.readLine()) != null) {
                        System.out.println("  [follow] " +
                                new String(line.getBytes(StandardCharsets.ISO_8859_1),
                                        StandardCharsets.UTF_8));
                    }
                    offset = raf.getFilePointer();
                }
                Thread.sleep(pollMillis);
            }
        }
        return offset;
    }

    static void main() throws IOException, InterruptedException {
        Path log = Files.createTempFile("tailf", ".log");
        try {
            Files.writeString(log, "line 1\nline 2\nline 3\nline 4\nline 5\n");

            System.out.println("=== Tail Follow ===");
            List<String> last3 = lastNLines(log, 3);
            System.out.println("Last 3 lines: " + last3);
            boolean tailOk = last3.equals(List.of("line 3", "line 4", "line 5"));

            long startOffset = Files.size(log);
            // Background appender: write 3 more lines, one per poll interval.
            Thread appender = new Thread(() -> {
                try {
                    for (int i = 6; i <= 8; i++) {
                        Thread.sleep(60);
                        Files.writeString(log, "line " + i + "\n",
                                java.nio.file.StandardOpenOption.APPEND);
                    }
                } catch (Exception ignored) {}
            });
            appender.start();

            System.out.println("Following for new appended lines...");
            follow(log, startOffset, 8, 50);   // bounded: 8 cycles, then returns
            appender.join();

            List<String> finalTail = lastNLines(log, 1);
            boolean followOk = finalTail.equals(List.of("line 8"));

            System.out.println((tailOk && followOk)
                    ? "PASSED: last-N seek and bounded follow both worked."
                    : "FAILED: tailOk=" + tailOk + " followOk=" + followOk);
        } finally {
            Files.deleteIfExists(log);
        }
    }
}
