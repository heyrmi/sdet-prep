package ra.hul.sdet.fileops;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Word Frequency Counter - Count words in a file and report the top-N, case-insensitive, punctuation-stripped.
 * Common SDET question: "Read a text file and print the N most frequent words, ignoring case and punctuation."
 *
 * Self-contained: builds a sample text temp file in main(), counts, prints top-N, cleans up.
 * Uses a min-heap of size N for an efficient top-N selection.
 */
public class Ques4_WordFrequencyCounter {

    /** A word and how many times it occurred. */
    public record WordCount(String word, long count) {}

    public static Map<String, Long> countWords(Path file) throws IOException {
        Map<String, Long> counts = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String raw : line.split("\\s+")) {
                    String word = raw.toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (!word.isEmpty()) counts.merge(word, 1L, Long::sum);
                }
            }
        }
        return counts;
    }

    /** Top-N by frequency (desc), ties broken alphabetically, using a bounded min-heap. */
    public static List<WordCount> topN(Map<String, Long> counts, int n) {
        Comparator<WordCount> byFreqThenWord =
                Comparator.comparingLong(WordCount::count).thenComparing(w -> w.word(),
                        Comparator.reverseOrder());
        PriorityQueue<WordCount> heap = new PriorityQueue<>(byFreqThenWord);
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            heap.offer(new WordCount(e.getKey(), e.getValue()));
            if (heap.size() > n) heap.poll();
        }
        List<WordCount> result = new ArrayList<>(heap);
        result.sort(Comparator.comparingLong(WordCount::count).reversed()
                .thenComparing(WordCount::word));
        return result;
    }

    static void main() throws IOException {
        Path file = Files.createTempFile("words", ".txt");
        try {
            String text = """
                    The quick brown fox jumps over the lazy dog.
                    The DOG was not amused; the fox, however, was quick!
                    Quick quick quick — the fox runs.
                    """;
            Files.writeString(file, text);

            Map<String, Long> counts = countWords(file);
            List<WordCount> top3 = topN(counts, 3);

            System.out.println("=== Word Frequency: top 3 ===");
            top3.forEach(w -> System.out.printf("%-8s %d%n", w.word(), w.count()));

            // "quick" x5, "the" x5, "fox" x3 ... top word by count+alpha tie-break is "quick".
            boolean ok = counts.get("quick") == 5
                    && counts.get("the") == 5
                    && counts.get("fox") == 3
                    && top3.getFirst().word().equals("quick");
            System.out.println(ok ? "PASSED: frequencies and top-N correct."
                    : "FAILED: unexpected counts " + top3);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
