package ra.hul.sdet.aiqa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Non-Determinism Control - getting a stable signal out of an unstable system.
 *
 * <p>The same prompt returns a different answer each call, so a single run is an anecdote, not a
 * measurement. Three techniques turn it back into something you can assert on:
 *
 * <ul>
 *   <li><b>pass@k</b> — probability that at least one of k samples is correct. The right metric
 *       when the product retries or offers alternatives (code generation, suggestions).</li>
 *   <li><b>Majority voting (self-consistency)</b> — sample n times, take the modal answer. Turns
 *       a 70%-accurate model into a much more accurate one for tasks with a single right answer,
 *       at n times the cost.</li>
 *   <li><b>Stability measurement</b> — how often does the same input produce the same output?
 *       Low stability on a task that should be deterministic is itself a bug report.</li>
 * </ul>
 *
 * <p>The judgement call an interviewer is listening for: <b>match the technique to the task.</b>
 * Majority voting on a summarisation task is meaningless — there is no modal summary. pass@k on a
 * task where the user only ever sees one answer is measuring something the user never experiences.
 *
 * <p>Self-contained: a seeded PRNG simulates model sampling, so every run of this file produces
 * identical numbers. That is itself the lesson — seeding is what makes a failure reproducible.
 */
public class Ques7_NonDeterminismControl {

    /** A stubbed model whose accuracy and determinism are configurable, driven by a seeded PRNG. */
    static final class StubModel {
        private final Random random;
        private final double accuracy;
        private final List<String> wrongAnswers;
        private final String correctAnswer;

        StubModel(long seed, double accuracy, String correctAnswer, List<String> wrongAnswers) {
            this.random = new Random(seed);   // seeded: the whole file is reproducible
            this.accuracy = accuracy;
            this.correctAnswer = correctAnswer;
            this.wrongAnswers = wrongAnswers;
        }

        String sample() {
            if (random.nextDouble() < accuracy) {
                return correctAnswer;
            }
            return wrongAnswers.get(random.nextInt(wrongAnswers.size()));
        }

        List<String> sampleN(int n) {
            List<String> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(sample());
            }
            return out;
        }
    }

    // ---------- pass@k ----------

    /**
     * Empirical pass@k: of {@code trials} independent attempts at drawing k samples, how often did
     * at least one sample match the expected answer?
     */
    static double passAtK(StubModel model, String expected, int k, int trials) {
        int successes = 0;
        for (int t = 0; t < trials; t++) {
            for (String sample : model.sampleN(k)) {
                if (sample.equals(expected)) {
                    successes++;
                    break;
                }
            }
        }
        return (double) successes / trials;
    }

    /**
     * Analytical pass@k for an independent per-sample success probability p:
     * {@code 1 - (1-p)^k}. Useful for reasoning about how many samples buy how much reliability
     * before you spend the tokens finding out.
     */
    static double theoreticalPassAtK(double p, int k) {
        return 1 - Math.pow(1 - p, k);
    }

    // ---------- majority voting ----------

    record Vote(String answer, int count, double confidence) {
    }

    /**
     * Self-consistency: take n samples and return the modal answer with its share of the vote.
     * The confidence is itself useful — a 3/5 win is a very different signal from 5/5, and is a
     * natural place to escalate to a larger model or a human.
     */
    static Vote majorityVote(List<String> samples) {
        if (samples.isEmpty()) {
            return new Vote("", 0, 0);
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : samples) {
            counts.merge(s, 1, Integer::sum);
        }
        String best = "";
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return new Vote(best, bestCount, (double) bestCount / samples.size());
    }

    /** Accuracy of majority-voted answers over many trials. */
    static double votedAccuracy(StubModel model, String expected, int samplesPerVote, int trials) {
        int correct = 0;
        for (int t = 0; t < trials; t++) {
            if (majorityVote(model.sampleN(samplesPerVote)).answer().equals(expected)) {
                correct++;
            }
        }
        return (double) correct / trials;
    }

    // ---------- stability ----------

    /**
     * Share of samples equal to the most common one. On a task that should be deterministic
     * (classification, extraction), low stability is a defect: fix the prompt, constrain the
     * output, or drop the temperature — do not paper over it with a looser assertion.
     */
    static double stability(List<String> samples) {
        return majorityVote(samples).confidence();
    }

    static void main() {
        int passed = 0, failed = 0;
        final String CORRECT = "BILLING";
        final List<String> WRONG = List.of("SHIPPING", "TECHNICAL", "ACCOUNT");

        System.out.println("=== 1) pass@k — when the product gets more than one shot ===\n");
        System.out.printf("%-6s %12s %12s%n", "k", "empirical", "theoretical");
        for (int k : new int[]{1, 2, 3, 5, 10}) {
            double empirical = passAtK(new StubModel(42, 0.60, CORRECT, WRONG), CORRECT, k, 2000);
            System.out.printf("%-6d %11.1f%% %11.1f%%%n",
                    k, empirical * 100, theoreticalPassAtK(0.60, k) * 100);
        }
        System.out.println("\n  A 60%-accurate model reaches ~99% at k=5. If your product retries or");
        System.out.println("  shows alternatives, pass@k is what the user actually experiences —");
        System.out.println("  and reporting pass@1 would badly understate it.");

        System.out.println("\n=== 2) Majority voting — one right answer, sampled n times ===\n");
        System.out.printf("%-16s %12s%n", "samples/vote", "accuracy");
        double single = votedAccuracy(new StubModel(7, 0.60, CORRECT, WRONG), CORRECT, 1, 2000);
        System.out.printf("%-16d %11.1f%%%n", 1, single * 100);
        double voted5 = votedAccuracy(new StubModel(7, 0.60, CORRECT, WRONG), CORRECT, 5, 2000);
        System.out.printf("%-16d %11.1f%%%n", 5, voted5 * 100);
        double voted9 = votedAccuracy(new StubModel(7, 0.60, CORRECT, WRONG), CORRECT, 9, 2000);
        System.out.printf("%-16d %11.1f%%%n", 9, voted9 * 100);
        System.out.println("\n  Voting buys accuracy at N times the token cost. Worth it for");
        System.out.println("  classification and extraction; MEANINGLESS for summarisation, where");
        System.out.println("  there is no modal answer to converge on.");

        System.out.println("\n=== 3) Stability — is this task even supposed to vary? ===\n");
        List<String> stable = new StubModel(1, 0.98, CORRECT, WRONG).sampleN(20);
        List<String> unstable = new StubModel(1, 0.45, CORRECT, WRONG).sampleN(20);
        System.out.printf("  well-specified task  stability %.0f%%%n", stability(stable) * 100);
        System.out.printf("  ambiguous task       stability %.0f%%  <- a DEFECT, not a test problem%n",
                stability(unstable) * 100);

        System.out.println("\n--- checks ---");

        // 1. pass@k rises with k.
        double p1 = passAtK(new StubModel(42, 0.60, CORRECT, WRONG), CORRECT, 1, 2000);
        double p5 = passAtK(new StubModel(42, 0.60, CORRECT, WRONG), CORRECT, 5, 2000);
        boolean c1 = p5 > p1;
        System.out.printf("pass@5 (%.1f%%) > pass@1 (%.1f%%)                : %s%n",
                p5 * 100, p1 * 100, c1);
        if (c1) passed++; else failed++;

        // 2. Empirical pass@k tracks the analytical form.
        boolean c2 = Math.abs(p5 - theoreticalPassAtK(0.60, 5)) < 0.05;
        System.out.printf("empirical pass@5 matches 1-(1-p)^k within 5 pts : %s%n", c2);
        if (c2) passed++; else failed++;

        // 3. Voting beats a single sample.
        boolean c3 = voted5 > single;
        System.out.printf("majority-of-5 (%.1f%%) beats single (%.1f%%)     : %s%n",
                voted5 * 100, single * 100, c3);
        if (c3) passed++; else failed++;

        // 4. More votes help, with diminishing returns.
        boolean c4 = voted9 >= voted5;
        System.out.printf("majority-of-9 (%.1f%%) >= majority-of-5          : %s%n", voted9 * 100, c4);
        if (c4) passed++; else failed++;

        // 5. Voting reports its own confidence, which is where escalation hooks in.
        Vote split = majorityVote(List.of("A", "A", "A", "B", "B"));
        boolean c5 = split.answer().equals("A") && Math.abs(split.confidence() - 0.6) < 1e-9;
        System.out.printf("3-2 split reports 60%% confidence                : %s%n", c5);
        System.out.println("    ^ escalate low-confidence votes to a bigger model or a human");
        if (c5) passed++; else failed++;

        // 6. Stability separates a well-specified task from an ambiguous one.
        boolean c6 = stability(stable) > 0.9 && stability(unstable) < 0.8;
        System.out.printf("stability distinguishes specified vs ambiguous   : %s%n", c6);
        if (c6) passed++; else failed++;

        // 7. THE point of the whole file: seeding makes it reproducible.
        List<String> runA = new StubModel(99, 0.5, CORRECT, WRONG).sampleN(50);
        List<String> runB = new StubModel(99, 0.5, CORRECT, WRONG).sampleN(50);
        boolean c7 = runA.equals(runB);
        System.out.printf("same seed reproduces the identical sample stream : %s%n", c7);
        System.out.println("    ^ seeding does not weaken a test — it converts 'fails sometimes'");
        System.out.println("      into 'fails with seed 99', which is the difference between a");
        System.out.println("      bug you can fix and a bug you retry");
        if (c7) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: non-determinism controlled via pass@k, voting, stability, and seeding."
                : "FAIL: non-determinism control mismatch.");
    }
}
