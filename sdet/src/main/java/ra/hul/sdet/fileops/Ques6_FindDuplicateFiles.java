package ra.hul.sdet.fileops;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Find Duplicate Files - Group files with identical content using SHA-256 (chunked hashing).
 * Common SDET question: "Find duplicate files by content, not name, and group them."
 *
 * Self-contained: builds a temp directory with some duplicate contents in main(), groups, cleans up.
 * Files are hashed in 8 KB chunks — large files never need to be fully buffered in memory.
 */
public class Ques6_FindDuplicateFiles {

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream in = Files.newInputStream(file)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Returns only the groups (keyed by hash) that contain more than one file. */
    public static Map<String, List<Path>> findDuplicates(Path root) throws IOException {
        Map<String, List<Path>> byHash = new HashMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString)).toList();
            for (Path f : files) {
                byHash.computeIfAbsent(sha256(f), k -> new ArrayList<>()).add(f);
            }
        }
        Map<String, List<Path>> dups = new HashMap<>();
        byHash.forEach((hash, list) -> { if (list.size() > 1) dups.put(hash, list); });
        return dups;
    }

    static void main() throws IOException {
        Path root = Files.createTempDirectory("dupes");
        try {
            Files.writeString(root.resolve("a.txt"), "identical content");
            Files.writeString(root.resolve("b.txt"), "identical content");   // dup of a
            Path sub = Files.createDirectory(root.resolve("nested"));
            Files.writeString(sub.resolve("c.txt"), "identical content");     // dup of a
            Files.writeString(root.resolve("unique.txt"), "one of a kind");

            Map<String, List<Path>> dups = findDuplicates(root);

            System.out.println("=== Find Duplicate Files (SHA-256) ===");
            dups.forEach((hash, group) -> {
                System.out.println("Group " + hash.substring(0, 12) + "...:");
                group.forEach(p -> System.out.println("   " + root.relativize(p)));
            });

            boolean ok = dups.size() == 1
                    && dups.values().iterator().next().size() == 3;
            System.out.println(ok ? "PASSED: one duplicate group of 3 files found."
                    : "FAILED: unexpected duplicate grouping.");
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
