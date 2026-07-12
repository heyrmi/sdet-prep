package ra.hul.sdet.fileops;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Merge Sorted Files - K-way merge of N sorted integer files into one sorted file, streaming.
 * Common SDET question: "Merge N sorted files without loading everything into memory (external sort)."
 *
 * Self-contained: builds N sorted temp files in main(), merges via a min-heap, verifies, cleans up.
 * Only one integer per input file is held in the heap at a time — memory is O(N), not O(total lines).
 */
public class Ques5_MergeSortedFiles {

    /** A live cursor over one sorted input file: the current value plus its reader. */
    private record Cursor(int value, BufferedReader reader) {}

    public static void merge(List<Path> inputs, Path output) throws IOException {
        List<BufferedReader> readers = new ArrayList<>();
        PriorityQueue<Cursor> heap =
                new PriorityQueue<>(Comparator.comparingInt(Cursor::value));
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            // Prime the heap with the first value from each file.
            for (Path in : inputs) {
                BufferedReader r = Files.newBufferedReader(in);
                readers.add(r);
                Integer first = nextInt(r);
                if (first != null) heap.offer(new Cursor(first, r));
            }
            // Repeatedly emit the smallest, then pull the next value from that file.
            while (!heap.isEmpty()) {
                Cursor c = heap.poll();
                writer.write(Integer.toString(c.value()));
                writer.newLine();
                Integer next = nextInt(c.reader());
                if (next != null) heap.offer(new Cursor(next, c.reader()));
            }
        } finally {
            for (BufferedReader r : readers) {
                try { r.close(); } catch (IOException ignored) {}
            }
        }
    }

    private static Integer nextInt(BufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) return Integer.parseInt(line);
        }
        return null;
    }

    private static Path writeSorted(int... nums) throws IOException {
        Path p = Files.createTempFile("sorted", ".txt");
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            for (int n : nums) { w.write(Integer.toString(n)); w.newLine(); }
        }
        return p;
    }

    static void main() throws IOException {
        List<Path> inputs = new ArrayList<>();
        Path output = Files.createTempFile("merged", ".txt");
        try {
            inputs.add(writeSorted(1, 4, 7, 10));
            inputs.add(writeSorted(2, 5, 8));
            inputs.add(writeSorted(3, 6, 9, 11, 12));

            merge(inputs, output);

            List<String> merged = Files.readAllLines(output);
            System.out.println("=== Merge Sorted Files: k-way merge ===");
            System.out.println("Merged output: " + merged);

            boolean sorted = true;
            for (int i = 1; i < merged.size(); i++) {
                if (Integer.parseInt(merged.get(i)) < Integer.parseInt(merged.get(i - 1))) {
                    sorted = false; break;
                }
            }
            boolean ok = sorted && merged.size() == 12
                    && merged.getFirst().equals("1") && merged.getLast().equals("12");
            System.out.println(ok ? "PASSED: 12 integers merged in sorted order."
                    : "FAILED: output not fully sorted/complete.");
        } finally {
            for (Path p : inputs) Files.deleteIfExists(p);
            Files.deleteIfExists(output);
        }
    }
}
