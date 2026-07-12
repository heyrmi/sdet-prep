package ra.hul.sdet.linux;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * File System Operations - create/copy/move files, walk a tree to find by pattern, size a dir, set POSIX perms.
 * Common SDET question: "Do common file operations in Java using java.nio.file".
 *
 * Self-contained: works entirely inside a temp directory it creates and deletes. No network.
 * POSIX permission step is skipped gracefully on non-POSIX filesystems (e.g. Windows). main() self-verifies.
 */
public class Ques2_FileSystemOperations {

    /** Recursively find files whose name matches the given glob (e.g. "*.log"). */
    public static List<Path> findByGlob(Path root, String glob) throws IOException {
        var matcher = root.getFileSystem().getPathMatcher("glob:" + glob);
        List<Path> hits = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p.getFileName()))
                    .forEach(hits::add);
        }
        return hits;
    }

    /** Total size in bytes of all regular files under root. */
    public static long directorySize(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    static void main() throws IOException {
        Path base = Files.createTempDirectory("sdet-fsops-");
        try {
            boolean pass = true;

            // 1) Create nested directories and files.
            Path sub = Files.createDirectories(base.resolve("logs/app"));
            Path a = Files.writeString(sub.resolve("service.log"), "line1\nline2\n");
            Files.writeString(sub.resolve("audit.log"), "audit\n");
            Files.writeString(base.resolve("readme.txt"), "hello");
            pass &= Files.isDirectory(sub) && Files.exists(a);

            // 2) Copy then move.
            Path copy = Files.copy(a, base.resolve("service-copy.log"));
            Path moved = Files.move(copy, base.resolve("service-moved.log"));
            pass &= Files.exists(moved) && !Files.exists(copy);

            // 3) Find by pattern (*.log) recursively.
            List<Path> logs = findByGlob(base, "*.log");
            System.out.println("Found " + logs.size() + " .log files: "
                    + logs.stream().map(p -> p.getFileName().toString()).sorted().toList());
            pass &= logs.size() == 3; // service.log, audit.log, service-moved.log

            // 4) Directory size (bytes) — non-empty.
            long size = directorySize(base);
            System.out.println("Directory size = " + size + " bytes");
            pass &= size > 0;

            // 5) Set POSIX permissions (rwxr-x---), when supported.
            if (Files.getFileStore(base).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
                Files.setPosixFilePermissions(a, perms);
                Set<PosixFilePermission> got = Files.getPosixFilePermissions(a);
                System.out.println("Permissions on service.log = " + PosixFilePermissions.toString(got));
                pass &= got.equals(perms);
            } else {
                System.out.println("POSIX permissions not supported here — step skipped.");
            }

            System.out.println(pass ? "PASS: create/copy/move/find/size/permissions all worked."
                    : "FAIL: file system operation mismatch.");
        } finally {
            deleteRecursively(base);
        }
    }

    // Unused illustrative visitor kept for reference on how to walk with a visitor instead of a stream.
    @SuppressWarnings("unused")
    private static long sizeViaVisitor(Path root) throws IOException {
        final long[] total = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                total[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });
        return total[0];
    }
}
