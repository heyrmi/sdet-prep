package ra.hul.sdet.fileops;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Grep Clone - Recursively search .txt/.log files for a regex pattern, printing file:line matches.
 * Common SDET question: "Implement a simplified grep with recursive traversal and streaming reads."
 *
 * Self-contained: builds a temp directory tree in main(), searches it, prints results, cleans up.
 * Files are read line-by-line (streaming) — never loaded whole into memory.
 */
public class Ques3_GrepClone {

    /** A single match: which file, which 1-based line number, the matching text. */
    public record Match(Path file, int lineNumber, String line) {}

    public static List<Match> grep(Path root, String regex) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        List<Match> matches = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".txt") || n.endsWith(".log");
                    })
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path file : files) {
                searchFile(file, pattern, matches);
            }
        }
        return matches;
    }

    private static void searchFile(Path file, Pattern pattern, List<Match> out) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (pattern.matcher(line).find()) {
                    out.add(new Match(file, lineNo, line));
                }
            }
        }
    }

    static void main() throws IOException {
        Path root = Files.createTempDirectory("grep-demo");
        try {
            Path sub = Files.createDirectory(root.resolve("logs"));
            Files.writeString(root.resolve("a.txt"),
                    "hello world\nerror: disk full\nall good here\n");
            Files.writeString(sub.resolve("app.log"),
                    "starting up\nERROR connection refused\nerror timeout\n");
            Files.writeString(root.resolve("skip.md"),
                    "error in markdown should be ignored\n");

            List<Match> results = grep(root, "(?i)error");

            System.out.println("=== Grep Clone: pattern '(?i)error' ===");
            for (Match mt : results) {
                System.out.printf("%s:%d: %s%n",
                        root.relativize(mt.file()), mt.lineNumber(), mt.line());
            }

            // 3 matches expected (a.txt line2, app.log line2 + line3); .md excluded.
            boolean ok = results.size() == 3
                    && results.stream().noneMatch(m -> m.file().toString().endsWith(".md"));
            System.out.println(ok ? "PASSED: found 3 matches in .txt/.log only."
                    : "FAILED: expected 3 matches, got " + results.size());
        } finally {
            deleteRecursively(root);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }
}
