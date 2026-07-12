package ra.hul.sdet.builds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * File Comparator Tool - Diff two text files line by line and emit a +/-/space diff with summary counts.
 * Common SDET question (machine-coding round): "Build a utility that diffs two files like `diff`,
 * marking added (+), removed (-), and unchanged (space) lines, and prints a summary."
 *
 * Self-contained: builds two temp files in main(), diffs them, prints the result, cleans up.
 * Uses an LCS (longest common subsequence) so unchanged lines line up correctly across insertions.
 */
public class Ques1_FileComparatorTool {

    /** One line of diff output plus a running summary. */
    public record DiffResult(List<String> lines, int added, int removed, int unchanged) {}

    /** Diff two files (text mode) using an LCS backtrace. Left file is "old", right file is "new". */
    public static DiffResult diff(Path oldFile, Path newFile) throws IOException {
        List<String> a = Files.readAllLines(oldFile);
        List<String> b = Files.readAllLines(newFile);
        return diffLines(a, b);
    }

    static DiffResult diffLines(List<String> a, List<String> b) {
        int n = a.size(), m = b.size();
        // lcs[i][j] = length of LCS of a[i..] and b[j..]
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (a.get(i).equals(b.get(j))) lcs[i][j] = lcs[i + 1][j + 1] + 1;
                else lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<String> out = new ArrayList<>();
        int added = 0, removed = 0, unchanged = 0;
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (a.get(i).equals(b.get(j))) {
                out.add("  " + a.get(i)); unchanged++; i++; j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.add("- " + a.get(i)); removed++; i++;
            } else {
                out.add("+ " + b.get(j)); added++; j++;
            }
        }
        while (i < n) { out.add("- " + a.get(i)); removed++; i++; }
        while (j < m) { out.add("+ " + b.get(j)); added++; j++; }
        return new DiffResult(out, added, removed, unchanged);
    }

    /** Binary comparison mode: byte-for-byte equality. */
    public static boolean binaryEquals(Path p1, Path p2) throws IOException {
        return Files.mismatch(p1, p2) == -1L;
    }

    static void main() throws IOException {
        Path f1 = Files.createTempFile("cmp-old", ".txt");
        Path f2 = Files.createTempFile("cmp-new", ".txt");
        try {
            Files.writeString(f1, """
                    alpha
                    beta
                    gamma
                    delta
                    """);
            Files.writeString(f2, """
                    alpha
                    beta-CHANGED
                    gamma
                    epsilon
                    delta
                    """);

            DiffResult r = diff(f1, f2);
            System.out.println("=== File Diff (- old, + new) ===");
            r.lines().forEach(System.out::println);
            System.out.printf("Summary: %d added, %d removed, %d unchanged%n",
                    r.added(), r.removed(), r.unchanged());

            // beta -> beta-CHANGED = 1 removed + 1 added; epsilon inserted = 1 added; alpha/gamma/delta unchanged
            boolean textOk = r.added() == 2 && r.removed() == 1 && r.unchanged() == 3;
            boolean binOk = binaryEquals(f1, f1) && !binaryEquals(f1, f2);
            System.out.println(textOk && binOk
                    ? "PASSED: diff counts and binary comparison match expected."
                    : "FAILED: diff mismatch (added=" + r.added() + " removed=" + r.removed()
                      + " unchanged=" + r.unchanged() + ").");
        } finally {
            Files.deleteIfExists(f1);
            Files.deleteIfExists(f2);
        }
    }
}
