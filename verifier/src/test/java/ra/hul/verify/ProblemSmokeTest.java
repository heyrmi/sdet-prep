package ra.hul.verify;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Executes the {@code static void main()} of every {@code Ques*} class in dsa/ and sdet/.
 *
 * <p>Every problem in this repo is self-verifying: its {@code main()} runs the examples and prints
 * the results. This harness is what turns that convention into an actual regression gate — a problem
 * that starts throwing (or hangs) fails the build instead of rotting silently.
 *
 * <p>Modes:
 * <ul>
 *   <li>{@code -Dverifier.offline=true} (default) — skips problems that need network or a browser.
 *       This is the PR gate: hermetic, fast, no flakes.</li>
 *   <li>{@code -Dverifier.offline=false} — runs everything, including networked problems.
 *       Used by the nightly job, where external-service flakiness is tolerable.</li>
 * </ul>
 *
 * <p>The skip list lives in {@code src/test/resources/network-dependent.txt} so adding a networked
 * problem is a one-line change rather than an edit here.
 */
class ProblemSmokeTest {

    private static final String[] ROOT_PACKAGES = {"ra/hul/dsa", "ra/hul/sdet"};

    private static final boolean OFFLINE =
            Boolean.parseBoolean(System.getProperty("verifier.offline", "true"));

    private static final int TIMEOUT_SECONDS =
            Integer.parseInt(System.getProperty("verifier.timeoutSeconds", "60"));

    // The pass/fail decision lives in OutcomeAnalyzer (src/main/java), which has its own unit
    // tests and is the target of the -Pmutation profile. Keeping the oracle out of the runner
    // means it can be tested directly rather than only through 277 end-to-end executions.

    @TestFactory
    Stream<DynamicTest> everyProblemRunsClean() throws IOException {
        List<String> classNames = discoverProblemClasses();
        Set<String> skip = loadSkipList();

        if (classNames.isEmpty()) {
            fail("No Ques* classes discovered on the classpath — did the reactor compile? "
                    + "Run `mvn clean compile` before `mvn -pl verifier test`.");
        }

        System.out.printf("[verifier] discovered %d problems, offline=%s, skip-list=%d%n",
                classNames.size(), OFFLINE, skip.size());

        return classNames.stream().map(name -> DynamicTest.dynamicTest(
                displayName(name),
                () -> {
                    if (OFFLINE && skip.contains(name)) {
                        // JUnit records this as aborted, not passed — the count stays honest.
                        org.junit.jupiter.api.Assumptions.assumeTrue(false,
                                "skipped in offline mode (needs network or a browser)");
                    }
                    runMain(name);
                }));
    }

    /**
     * Invokes {@code main()} on a worker thread with output captured, so a passing run stays quiet
     * and a failing one reports what the problem printed before it died.
     */
    private void runMain(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Method main = findMain(clazz);

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        InputStream originalIn = System.in;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "verify-" + className);
            t.setDaemon(true);   // a hung problem must not keep the JVM alive
            return t;
        });

        try {
            System.setOut(sink);
            System.setErr(sink);
            System.setIn(InputStream.nullInputStream());

            Future<?> future = executor.submit(() -> {
                try {
                    main.invoke(null);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });

            try {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                fail(className + " did not finish within " + TIMEOUT_SECONDS + "s — "
                        + "likely an infinite loop or a blocking call.\n" + tail(captured));
            }
        } catch (Exception e) {
            // Restore before failing so the assertion message actually reaches the console.
            System.setOut(originalOut);
            System.setErr(originalErr);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail(className + " threw " + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage() + "\n" + tail(captured), cause);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            System.setIn(originalIn);
            executor.shutdownNow();
        }

        String output = captured.toString(StandardCharsets.UTF_8);
        OutcomeAnalyzer.Verdict verdict = OutcomeAnalyzer.analyze(output);
        if (verdict != OutcomeAnalyzer.Verdict.OK) {
            fail(OutcomeAnalyzer.describe(verdict, className) + "\n" + tail(captured));
        }
    }

    private static Method findMain(Class<?> clazz) throws NoSuchMethodException {
        // Repo convention is the Java 25 no-args `static void main()`, but accept the classic
        // signature too so this harness keeps working if a problem uses it.
        try {
            Method m = clazz.getDeclaredMethod("main");
            m.setAccessible(true);   // convention is package-private
            return m;
        } catch (NoSuchMethodException ignored) {
            Method m = clazz.getDeclaredMethod("main", String[].class);
            m.setAccessible(true);
            return m;
        }
    }

    /**
     * Finds compiled {@code Ques*} classes on the classpath.
     *
     * <p>Handles both shapes the reactor produces: a full {@code mvn test} passes sibling modules as
     * {@code target/classes} directories, while {@code mvn -pl verifier test} resolves them to
     * installed jars. Miss either one and discovery silently returns nothing.
     */
    private static List<String> discoverProblemClasses() throws IOException {
        List<String> found = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            Path root = Path.of(entry);
            if (Files.isDirectory(root)) {
                scanDirectory(root, found);
            } else if (entry.endsWith(".jar") && Files.isRegularFile(root)) {
                scanJar(root, found);
            }
        }
        found.sort(String::compareTo);
        return found;
    }

    private static void scanDirectory(Path root, List<String> found) throws IOException {
        for (String pkg : ROOT_PACKAGES) {
            Path pkgRoot = root.resolve(pkg);
            if (!Files.isDirectory(pkgRoot)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(pkgRoot)) {
                walk.filter(Files::isRegularFile)
                        .map(p -> root.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                        .map(ProblemSmokeTest::toProblemClassName)
                        .filter(java.util.Objects::nonNull)
                        .forEach(found::add);
            }
        }
    }

    private static void scanJar(Path jar, List<String> found) throws IOException {
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jar.toFile())) {
            jarFile.stream()
                    .map(java.util.jar.JarEntry::getName)
                    .filter(name -> Stream.of(ROOT_PACKAGES).anyMatch(name::startsWith))
                    .map(ProblemSmokeTest::toProblemClassName)
                    .filter(java.util.Objects::nonNull)
                    .forEach(found::add);
        }
    }

    /** Maps a classpath-relative resource path to a problem FQCN, or null if it is not one. */
    private static String toProblemClassName(String resourcePath) {
        if (!resourcePath.endsWith(".class") || resourcePath.contains("$")) {
            return null;   // nested helper classes are not entry points
        }
        String fqcn = resourcePath.substring(0, resourcePath.length() - ".class".length())
                .replace('/', '.');
        String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        return simpleName.startsWith("Ques") ? fqcn : null;
    }

    private static Set<String> loadSkipList() throws IOException {
        Set<String> skip = new HashSet<>();
        try (InputStream in = ProblemSmokeTest.class.getResourceAsStream("/network-dependent.txt")) {
            if (in == null) {
                return skip;
            }
            new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(skip::add);
        }
        return skip;
    }

    private static String displayName(String fqcn) {
        String[] parts = fqcn.split("\\.");
        return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }

    /** Last 25 lines of captured output — enough to debug, short enough to read in CI logs. */
    private static String tail(ByteArrayOutputStream captured) {
        List<String> lines = captured.toString(StandardCharsets.UTF_8).lines().toList();
        int from = Math.max(0, lines.size() - 25);
        return "--- output ---\n" + String.join("\n", lines.subList(from, lines.size()));
    }
}
