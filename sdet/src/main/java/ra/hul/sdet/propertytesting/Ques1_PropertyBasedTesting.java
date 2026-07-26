package ra.hul.sdet.propertytesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Property-Based Testing - generating the inputs you would never have thought to write.
 *
 * <p>Example-based tests check the cases you imagined. That is the problem: the bug is, by
 * definition, in a case you did not imagine. Property-based testing inverts this — you state a
 * property that must hold for ALL inputs, and a generator hunts for a counterexample.
 *
 * <pre>
 *   example-based : assertEquals(3, add(1, 2))
 *   property-based: for all a, b :  add(a, b) == add(b, a)
 * </pre>
 *
 * <p>The properties worth knowing by name, because they are what you reach for when someone asks
 * "what property would you even test?":
 *
 * <ul>
 *   <li><b>Round-trip</b> — {@code decode(encode(x)) == x}. The highest-value property in
 *       existence: serialisers, parsers, compressors, and encoders all have one.</li>
 *   <li><b>Invariant</b> — something true before and after: a sort preserves length and
 *       multiset contents.</li>
 *   <li><b>Idempotence</b> — {@code f(f(x)) == f(x)}: sorting, normalising, deduplicating.</li>
 *   <li><b>Commutativity / associativity</b> — order does not matter.</li>
 *   <li><b>Oracle</b> — compare against a slow, obviously-correct reference implementation.</li>
 *   <li><b>Metamorphic</b> — you cannot state the right answer, but you know how it must CHANGE
 *       when the input changes. See {@link Ques3_MetamorphicTesting}.</li>
 * </ul>
 *
 * <p>Interview angle: "how would you test a function whose correct output you cannot easily
 * predict?" — properties, not examples. And note that a property test that finds nothing is still
 * evidence, whereas a passing example test is only evidence about that example.
 *
 * <p>Self-contained: a seeded generator, so failures are reproducible. Ships with a deliberately
 * buggy implementation to prove the harness actually catches things.
 */
public class Ques1_PropertyBasedTesting {

    /** Outcome of checking one property over many generated inputs. */
    record PropertyResult<T>(boolean held, int casesRun, T counterexample, long seed) {
    }

    /**
     * Checks a property over generated inputs, stopping at the first counterexample.
     *
     * @param seed      the PRNG seed — reported on failure so the run reproduces exactly
     * @param cases     how many inputs to try
     * @param generator produces one input from a Random
     * @param property  must hold for every generated input
     */
    static <T> PropertyResult<T> forAll(long seed, int cases,
                                        Function<Random, T> generator,
                                        Predicate<T> property) {
        Random random = new Random(seed);
        for (int i = 0; i < cases; i++) {
            T input = generator.apply(random);
            boolean ok;
            try {
                ok = property.test(input);
            } catch (RuntimeException e) {
                ok = false;   // an exception is a failed property, not a harness error
            }
            if (!ok) {
                return new PropertyResult<>(false, i + 1, input, seed);
            }
        }
        return new PropertyResult<>(true, cases, null, seed);
    }

    // ---------- generators ----------

    static int[] randomIntArray(Random r) {
        int length = r.nextInt(20);
        int[] out = new int[length];
        for (int i = 0; i < length; i++) {
            out[i] = r.nextInt(201) - 100;   // -100..100, so duplicates are common
        }
        return out;
    }

    static String randomString(Random r) {
        int length = r.nextInt(15);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // A deliberately nasty alphabet: commas and quotes are what break naive CSV code.
            sb.append("ab,\"\n ".charAt(r.nextInt(6)));
        }
        return sb.toString();
    }

    // ---------- the code under test ----------

    /** A correct insertion sort. */
    static int[] sortCorrect(int[] input) {
        int[] a = input.clone();
        for (int i = 1; i < a.length; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
        return a;
    }

    /**
     * A subtly broken sort: it drops duplicates. Every hand-written example test using distinct
     * values passes, which is exactly how this class of bug survives code review.
     */
    static int[] sortBuggy(int[] input) {
        int[] sorted = sortCorrect(input);
        List<Integer> unique = new ArrayList<>();
        for (int v : sorted) {
            if (unique.isEmpty() || unique.get(unique.size() - 1) != v) {
                unique.add(v);
            }
        }
        return unique.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Correct CSV field encoding: quote when needed, and double any embedded quote. */
    static String csvEncode(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    static String csvDecode(String encoded) {
        if (encoded.startsWith("\"") && encoded.endsWith("\"") && encoded.length() >= 2) {
            return encoded.substring(1, encoded.length() - 1).replace("\"\"", "\"");
        }
        return encoded;
    }

    /** A broken encoder: it quotes, but forgets to escape embedded quotes. */
    static String csvEncodeBuggy(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field + "\"";
        }
        return field;
    }

    // ---------- properties ----------

    static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    static boolean sameMultiset(int[] a, int[] b) {
        int[] x = a.clone(), y = b.clone();
        Arrays.sort(x);
        Arrays.sort(y);
        return Arrays.equals(x, y);
    }

    static void main() {
        int passed = 0, failed = 0;
        final long SEED = 20260724L;
        final int CASES = 1000;

        System.out.println("=== Property-based testing (seed " + SEED + ", " + CASES + " cases) ===\n");

        // --- Property 1: output is ordered. The buggy sort satisfies this! ---
        PropertyResult<int[]> ordered = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomIntArray,
                a -> isSorted(sortBuggy(a)));
        System.out.println("[buggy sort] property 'output is ordered'   -> held: " + ordered.held());
        System.out.println("    A weak property passes a broken implementation. Ordering alone");
        System.out.println("    says nothing about whether the elements survived.");
        boolean c1 = ordered.held();
        if (c1) passed++; else failed++;

        // --- Property 2: the invariant that actually catches it ---
        PropertyResult<int[]> preserved = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomIntArray,
                a -> sameMultiset(a, sortBuggy(a)));
        System.out.println("\n[buggy sort] property 'preserves elements'  -> held: " + preserved.held());
        System.out.printf("    counterexample after %d cases: %s%n",
                preserved.casesRun(), Arrays.toString(preserved.counterexample()));
        System.out.printf("    became: %s%n", Arrays.toString(sortBuggy(preserved.counterexample())));
        boolean c2 = !preserved.held() && preserved.counterexample() != null;
        if (c2) passed++; else failed++;

        // --- Property 3: the correct sort satisfies both ---
        PropertyResult<int[]> good1 = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomIntArray, a -> isSorted(sortCorrect(a)));
        PropertyResult<int[]> good2 = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomIntArray, a -> sameMultiset(a, sortCorrect(a)));
        System.out.println("\n[correct sort] both properties hold over " + CASES + " cases -> "
                + (good1.held() && good2.held()));
        boolean c3 = good1.held() && good2.held();
        if (c3) passed++; else failed++;

        // --- Property 4: idempotence ---
        PropertyResult<int[]> idempotent = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomIntArray,
                a -> Arrays.equals(sortCorrect(sortCorrect(a)), sortCorrect(a)));
        System.out.println("[correct sort] idempotent: sort(sort(x)) == sort(x) -> " + idempotent.held());
        boolean c4 = idempotent.held();
        if (c4) passed++; else failed++;

        // --- Property 5: round-trip, the highest-value property there is ---
        System.out.println("\n=== Round-trip: decode(encode(x)) == x ===");
        PropertyResult<String> roundTripBuggy = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomString,
                s -> csvDecode(csvEncodeBuggy(s)).equals(s));
        System.out.println("[buggy encoder]   round-trip holds -> " + roundTripBuggy.held());
        System.out.printf("    counterexample after %d cases: %s%n",
                roundTripBuggy.casesRun(), escape(roundTripBuggy.counterexample()));
        System.out.printf("    encoded: %s   decoded back as: %s%n",
                escape(csvEncodeBuggy(roundTripBuggy.counterexample())),
                escape(csvDecode(csvEncodeBuggy(roundTripBuggy.counterexample()))));
        boolean c5 = !roundTripBuggy.held();
        if (c5) passed++; else failed++;

        PropertyResult<String> roundTripGood = forAll(SEED, CASES,
                Ques1_PropertyBasedTesting::randomString,
                s -> csvDecode(csvEncode(s)).equals(s));
        System.out.println("[correct encoder] round-trip holds -> " + roundTripGood.held());
        boolean c6 = roundTripGood.held();
        if (c6) passed++; else failed++;

        // --- Property 6: reproducibility ---
        PropertyResult<int[]> runA = forAll(999L, 500,
                Ques1_PropertyBasedTesting::randomIntArray, a -> sameMultiset(a, sortBuggy(a)));
        PropertyResult<int[]> runB = forAll(999L, 500,
                Ques1_PropertyBasedTesting::randomIntArray, a -> sameMultiset(a, sortBuggy(a)));
        boolean c7 = Arrays.equals(runA.counterexample(), runB.counterexample())
                && runA.casesRun() == runB.casesRun();
        System.out.println("\nsame seed reproduces the identical counterexample -> " + c7);
        System.out.println("    ^ report the SEED with every failure; that is what turns a");
        System.out.println("      generated failure into a permanent regression test");
        if (c7) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: properties catch bugs that example-based tests miss, reproducibly."
                : "FAIL: property-based testing mismatch.");
    }

    private static String escape(String s) {
        return s == null ? "null" : "\"" + s.replace("\n", "\\n") + "\"";
    }
}
