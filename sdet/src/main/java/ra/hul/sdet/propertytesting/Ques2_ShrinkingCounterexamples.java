package ra.hul.sdet.propertytesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Shrinking - turning a 200-element counterexample into a 2-element one.
 *
 * <p>A generator that finds a bug usually hands you something unreadable:
 *
 * <pre>
 *   FAILED on [-73, 12, 0, 88, -5, 41, 41, -100, 7, ...]  (183 more)
 * </pre>
 *
 * <p>Nobody can debug that. The value of a property-based framework is not really the generation —
 * it is the <b>shrinking</b>: automatically searching for the SMALLEST input that still fails.
 *
 * <pre>
 *   FAILED on [41, 41]
 * </pre>
 *
 * <p>Now the bug is obvious. This is the single feature that makes property-based testing
 * practical rather than merely clever, and it is the thing people miss when they hand-roll a
 * generator loop.
 *
 * <p>The strategy is greedy: repeatedly try a set of "simpler" candidates; whenever one still
 * fails, adopt it and start again. Stop when no candidate reproduces the failure — you are then at
 * a local minimum, which in practice is small enough to read.
 *
 * <p>Interview angle: "generated tests give unreadable failures" — the answer is shrinking, plus
 * reporting the seed.
 *
 * <p>Self-contained: seeded, no network.
 */
public class Ques2_ShrinkingCounterexamples {

    /** Candidate simplifications of an int array, cheapest-to-understand first. */
    static List<int[]> shrinkCandidates(int[] input) {
        List<int[]> candidates = new ArrayList<>();
        if (input.length == 0) {
            return candidates;
        }

        // 1) Halve the length — the biggest win, tried first.
        if (input.length > 1) {
            candidates.add(Arrays.copyOfRange(input, 0, input.length / 2));
            candidates.add(Arrays.copyOfRange(input, input.length / 2, input.length));
        }

        // 2) Remove one element at a time.
        for (int i = 0; i < input.length; i++) {
            int[] smaller = new int[input.length - 1];
            System.arraycopy(input, 0, smaller, 0, i);
            System.arraycopy(input, i + 1, smaller, i, input.length - i - 1);
            candidates.add(smaller);
        }

        // 3) GLOBAL value moves, applied to every element at once.
        //
        //    These exist because per-element shrinking gets stuck at a local minimum. For a
        //    "drops duplicates" bug, [18, 18] cannot shrink one element at a time — zeroing
        //    either one destroys the duplicate and the property starts holding again. Only a
        //    simultaneous move reaches [0, 0]. Real shrinkers all carry moves like these.
        int[] allZero = new int[input.length];
        candidates.add(allZero);

        int[] allHalved = input.clone();
        boolean anyHalved = false;
        for (int i = 0; i < allHalved.length; i++) {
            if (allHalved[i] != 0) {
                allHalved[i] /= 2;
                anyHalved = true;
            }
        }
        if (anyHalved) {
            candidates.add(allHalved);
        }

        int[] allPositive = input.clone();
        boolean anyNegative = false;
        for (int i = 0; i < allPositive.length; i++) {
            if (allPositive[i] < 0) {
                allPositive[i] = -allPositive[i];
                anyNegative = true;
            }
        }
        if (anyNegative) {
            candidates.add(allPositive);
        }

        // 4) Simplify individual values toward zero. A counterexample of [41, 41] is good;
        //    [1, 1] is better, because it strips away any suspicion that 41 mattered.
        for (int i = 0; i < input.length; i++) {
            if (input[i] != 0) {
                int[] zeroed = input.clone();
                zeroed[i] = 0;
                candidates.add(zeroed);
            }
            if (input[i] > 1 || input[i] < -1) {
                int[] halved = input.clone();
                halved[i] = input[i] / 2;
                candidates.add(halved);
            }
            if (input[i] < 0) {
                int[] positive = input.clone();
                positive[i] = -input[i];
                candidates.add(positive);
            }
        }
        return candidates;
    }

    /** The result of a shrink run. */
    record Shrunk(int[] original, int[] minimal, int steps) {
    }

    /**
     * Greedily shrinks a failing input while the property still fails.
     *
     * @param failing  an input known to violate the property
     * @param property the property under test (returns true when it HOLDS)
     */
    static Shrunk shrink(int[] failing, Predicate<int[]> property) {
        int[] current = failing;
        int steps = 0;

        boolean improved = true;
        while (improved) {
            improved = false;
            for (int[] candidate : shrinkCandidates(current)) {
                // A candidate identical to the current input is not progress. Without this
                // guard the global "set everything to zero" move re-adopts an already-zeroed
                // input forever — a real and easy-to-miss non-termination bug.
                if (Arrays.equals(candidate, current)) {
                    continue;
                }
                if (!property.test(candidate)) {
                    // Still fails, and it is simpler — adopt it and restart the search.
                    current = candidate;
                    steps++;
                    improved = true;
                    break;
                }
            }
        }
        return new Shrunk(failing, current, steps);
    }

    // ---------- code under test ----------

    /** Drops duplicates, so it is wrong whenever the input has any. */
    static int[] sortBuggy(int[] input) {
        int[] sorted = input.clone();
        Arrays.sort(sorted);
        List<Integer> unique = new ArrayList<>();
        for (int v : sorted) {
            if (unique.isEmpty() || unique.get(unique.size() - 1) != v) {
                unique.add(v);
            }
        }
        return unique.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The property: sorting preserves the element count. */
    static boolean preservesLength(int[] a) {
        return sortBuggy(a).length == a.length;
    }

    /** Overflows for large sums, but only for large ones. */
    static int sumBuggy(int[] a) {
        int total = 0;
        for (int v : a) {
            total += v;   // int overflow
        }
        return total;
    }

    static boolean sumMatchesLong(int[] a) {
        long expected = 0;
        for (int v : a) {
            expected += v;
        }
        return sumBuggy(a) == expected;
    }

    static void main() {
        int passed = 0, failed = 0;
        Random random = new Random(20260724L);

        System.out.println("=== Shrinking a generated counterexample ===\n");

        // Build a big, messy failing input the way a generator would.
        int[] messy = new int[60];
        for (int i = 0; i < messy.length; i++) {
            messy[i] = random.nextInt(41) - 20;
        }
        messy[17] = 7;
        messy[43] = 7;   // guarantee a duplicate

        System.out.println("generator found a failure on " + messy.length + " elements:");
        System.out.println("  " + Arrays.toString(messy));
        System.out.println("  ^ nobody can debug this");

        Shrunk result = shrink(messy, Ques2_ShrinkingCounterexamples::preservesLength);
        System.out.println("\nafter " + result.steps() + " shrink steps:");
        System.out.println("  " + Arrays.toString(result.minimal()));
        System.out.println("  ^ the bug is now self-evident: duplicates are dropped");

        System.out.println("\n--- checks ---");

        // 1. The shrunk input must STILL fail. A shrinker that returns a passing input has
        //    "fixed" the bug by losing it, which is worse than not shrinking at all.
        boolean c1 = !preservesLength(result.minimal());
        System.out.println("shrunk input still reproduces the failure  : " + c1);
        if (c1) passed++; else failed++;

        // 2. It must be dramatically smaller.
        boolean c2 = result.minimal().length < messy.length / 5;
        System.out.printf("shrunk from %d to %d elements               : %s%n",
                messy.length, result.minimal().length, c2);
        if (c2) passed++; else failed++;

        // 3. The minimal counterexample for "drops duplicates" is two equal elements.
        boolean c3 = result.minimal().length == 2
                && result.minimal()[0] == result.minimal()[1];
        System.out.println("reduced to exactly two equal elements      : " + c3);
        if (c3) passed++; else failed++;

        // 4. Values simplified toward zero as well as length.
        boolean c4 = Math.abs(result.minimal()[0]) <= 1;
        System.out.printf("values simplified toward zero (%d)          : %s%n",
                result.minimal()[0], c4);
        System.out.println("    ^ [0, 0] proves the value was irrelevant; [7, 7] leaves doubt");
        if (c4) passed++; else failed++;

        // 5. Shrinking is deterministic.
        Shrunk again = shrink(messy, Ques2_ShrinkingCounterexamples::preservesLength);
        boolean c5 = Arrays.equals(result.minimal(), again.minimal());
        System.out.println("shrinking is deterministic                 : " + c5);
        if (c5) passed++; else failed++;

        // 6. A different bug shrinks to a different shape. Overflow needs LARGE values, so the
        //    shrinker must not blindly drive everything to zero.
        System.out.println("\n=== A bug that needs large values ===");
        int[] overflow = {Integer.MAX_VALUE - 10, 100, 5, 3, 2, 1};
        Shrunk overflowShrunk = shrink(overflow, Ques2_ShrinkingCounterexamples::sumMatchesLong);
        System.out.println("  shrunk to: " + Arrays.toString(overflowShrunk.minimal()));
        boolean c6 = !sumMatchesLong(overflowShrunk.minimal())
                && overflowShrunk.minimal().length <= overflow.length;
        System.out.println("overflow counterexample still overflows    : " + c6);
        System.out.println("    ^ shrinking is guided by the PROPERTY, not by a fixed notion of");
        System.out.println("      'small' — it cannot zero out the value the bug depends on");
        if (c6) passed++; else failed++;

        // 7. An already-minimal input is left alone.
        int[] minimal = {0, 0};
        Shrunk noop = shrink(minimal, Ques2_ShrinkingCounterexamples::preservesLength);
        boolean c7 = Arrays.equals(noop.minimal(), minimal) && noop.steps() == 0;
        System.out.println("\nalready-minimal input is unchanged         : " + c7);
        if (c7) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: shrinking produces minimal, still-failing, deterministic counterexamples."
                : "FAIL: shrinking mismatch.");
    }
}
