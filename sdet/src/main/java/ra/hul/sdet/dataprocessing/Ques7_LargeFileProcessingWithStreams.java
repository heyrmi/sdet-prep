package ra.hul.sdet.dataprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Large File Processing with Streams - Aggregate a large CSV lazily with Files.lines(),
 * without loading the whole file into memory.
 * Common SDET question: "A multi-GB CSV won't fit in RAM — compute per-category aggregates and
 * filter rows using Java Streams (Files.lines + groupingBy + summarizingDouble)."
 *
 * Self-contained: generates a large temp CSV (default 500,000 rows), streams it, prints aggregates
 * and a rough memory reading, then deletes the temp file. NO network.
 */
public class Ques7_LargeFileProcessingWithStreams {

    static final String[] CATEGORIES = {"BOOKS", "TOYS", "FOOD", "TECH", "HOME"};

    /** Write a large CSV: id,category,amount — one row at a time (constant memory to WRITE). */
    static Path generateCsv(int rows) throws IOException {
        Path file = Files.createTempFile("large-sales-", ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("id,category,amount\n");
            for (int i = 0; i < rows; i++) {
                String category = CATEGORIES[i % CATEGORIES.length];
                // Deterministic pseudo amounts so results are checkable.
                double amount = 10 + (i % 100);
                w.write(i + "," + category + "," + amount + "\n");
            }
        }
        return file;
    }

    /**
     * Stream the file lazily and compute summarizing stats per category.
     * Files.lines() is a lazily-populated stream backed by the file — rows are NOT all held in memory.
     */
    static Map<String, DoubleSummaryStatistics> aggregateByCategory(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return lines
                    .skip(1) // header
                    .map(line -> line.split(","))
                    .collect(Collectors.groupingBy(
                            parts -> parts[1],
                            TreeMap::new,
                            Collectors.summarizingDouble(parts -> Double.parseDouble(parts[2]))));
        }
    }

    /** Lazily filter + count rows above a threshold without materializing the list. */
    static long countAbove(Path file, double threshold) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.skip(1)
                    .mapToDouble(line -> Double.parseDouble(line.split(",")[2]))
                    .filter(a -> a > threshold)
                    .count();
        }
    }

    /** Total row count via lazy stream (memory-safe on huge files). */
    static long countRows(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.skip(1).count();
        }
    }

    static void main() throws IOException {
        int rows = 500_000;
        Path file = generateCsv(rows);
        try {
            System.out.printf("Generated temp CSV: %s (%,d data rows, %,d bytes)%n",
                    file, rows, Files.size(file));

            Runtime rt = Runtime.getRuntime();
            System.gc();
            long before = rt.totalMemory() - rt.freeMemory();
            long start = System.nanoTime();

            Map<String, DoubleSummaryStatistics> stats = aggregateByCategory(file);

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            long after = rt.totalMemory() - rt.freeMemory();

            System.out.println("\nPer-category aggregates (Files.lines + groupingBy + summarizingDouble):");
            double grandTotal = 0;
            long grandCount = 0;
            for (var e : stats.entrySet()) {
                DoubleSummaryStatistics s = e.getValue();
                System.out.printf("  %-6s count=%,d  sum=%,.1f  avg=%.2f  min=%.1f  max=%.1f%n",
                        e.getKey(), s.getCount(), s.getSum(), s.getAverage(), s.getMin(), s.getMax());
                grandTotal += s.getSum();
                grandCount += s.getCount();
            }

            long above = countAbove(file, 100.0);
            long total = countRows(file);

            System.out.printf("%nRows with amount > 100: %,d%n", above);
            System.out.printf("Streamed in %d ms; heap delta during aggregation ~%,d KB (whole file NOT loaded)%n",
                    elapsedMs, Math.max(0, (after - before)) / 1024);

            // Verify against the deterministic generator.
            // amounts cycle 10..109; each category gets rows/5 rows.
            check("all rows counted once", grandCount == rows && total == rows);
            check("5 categories present", stats.size() == 5);
            check("even split per category", stats.values().stream().allMatch(s -> s.getCount() == rows / 5));
            // amount in [10,109]; > 100 means values 101..109 => 9 of every 100 rows.
            check("filter count matches formula", above == (long) rows * 9 / 100);
            // avg per category: amounts are (i%100)+10 for i in that category's arithmetic sequence.
            check("global min/max correct",
                    stats.values().stream().mapToDouble(DoubleSummaryStatistics::getMin).min().orElse(-1) == 10.0
                    && stats.values().stream().mapToDouble(DoubleSummaryStatistics::getMax).max().orElse(-1) == 109.0);

            System.out.println("\nPASSED: large CSV streamed lazily with correct grouped aggregates.");
        } finally {
            Files.deleteIfExists(file);
            System.out.println("Cleaned up temp file.");
        }
    }

    static void check(String label, boolean ok) {
        System.out.println((ok ? "  PASS: " : "  FAIL: ") + label);
        if (!ok) throw new AssertionError("Check failed: " + label);
    }
}
