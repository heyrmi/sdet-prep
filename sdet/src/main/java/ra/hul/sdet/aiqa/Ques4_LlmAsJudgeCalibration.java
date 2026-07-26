package ra.hul.sdet.aiqa;

import java.util.List;

/**
 * LLM-as-Judge Calibration - proving your grader is trustworthy before you trust it.
 *
 * <p>For subjective dimensions (tone, helpfulness, faithfulness) there is no assertion to write,
 * so a second model grades the output. That works — but an uncalibrated judge is just a confident
 * random number generator, and gating merges on it is worse than not gating at all.
 *
 * <p>The discipline:
 * <ol>
 *   <li>Have humans label a holdout set.</li>
 *   <li>Measure judge-vs-human <b>agreement</b>. The working target is <b>85-90%</b>.</li>
 *   <li>Report <b>Cohen's kappa</b>, not raw agreement — raw agreement is inflated by chance,
 *       badly so when the classes are imbalanced.</li>
 *   <li>Re-check whenever the judge model or judge prompt changes.</li>
 * </ol>
 *
 * <p>And know the documented biases: <b>position</b> (prefers whichever came first),
 * <b>verbosity</b> (prefers longer), <b>self-preference</b> (prefers its own family's text), and
 * <b>confidence</b> (prefers assertive prose even when wrong — the dangerous one, because a human
 * reviewer shares it).
 *
 * <p>Interview angle: "how do you know the judge is right?" — this. A judge score reported without
 * its agreement rate is decoration.
 *
 * <p>Self-contained: fixed human labels and simulated judge verdicts, no model calls.
 */
public class Ques4_LlmAsJudgeCalibration {

    /** One graded item: what a human said, what the judge said, plus attributes for bias checks. */
    record Judgement(
            String itemId,
            boolean humanPass,
            boolean judgePass,
            int responseLength,
            int positionShown   // 1 = shown first, 2 = shown second
    ) {
        boolean agrees() {
            return humanPass == judgePass;
        }
    }

    /** A 2x2 confusion matrix of judge vs human. */
    record Confusion(int truePos, int falsePos, int trueNeg, int falseNeg) {
        int total() {
            return truePos + falsePos + trueNeg + falseNeg;
        }

        /** Raw agreement — inflated by chance, which is why kappa exists. */
        double rawAgreement() {
            return total() == 0 ? 0 : (double) (truePos + trueNeg) / total();
        }

        /**
         * The judge's false-positive rate: how often it passes something a human failed.
         * This is the number that matters most — a judge that waves through bad output is
         * actively harmful, whereas one that is merely strict just costs you some review time.
         */
        double falsePassRate() {
            int humanFails = trueNeg + falsePos;
            return humanFails == 0 ? 0 : (double) falsePos / humanFails;
        }
    }

    static Confusion confusionOf(List<Judgement> judgements) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (Judgement j : judgements) {
            if (j.humanPass() && j.judgePass()) tp++;
            else if (!j.humanPass() && j.judgePass()) fp++;
            else if (!j.humanPass() && !j.judgePass()) tn++;
            else fn++;
        }
        return new Confusion(tp, fp, tn, fn);
    }

    /**
     * Cohen's kappa: agreement corrected for what chance alone would produce.
     *
     * <pre>
     *   kappa = (Po - Pe) / (1 - Pe)
     * </pre>
     *
     * where Po is observed agreement and Pe is expected-by-chance agreement.
     * Conventional reading: &lt;0.20 poor, 0.21-0.40 fair, 0.41-0.60 moderate,
     * 0.61-0.80 substantial, &gt;0.80 almost perfect.
     */
    static double cohensKappa(Confusion c) {
        int n = c.total();
        if (n == 0) {
            return 0;
        }
        double po = c.rawAgreement();

        double humanPassRate = (double) (c.truePos() + c.falseNeg()) / n;
        double judgePassRate = (double) (c.truePos() + c.falsePos()) / n;
        double pe = humanPassRate * judgePassRate
                + (1 - humanPassRate) * (1 - judgePassRate);

        return pe == 1.0 ? 1.0 : (po - pe) / (1 - pe);
    }

    static String kappaVerdict(double kappa) {
        if (kappa > 0.80) return "almost perfect";
        if (kappa > 0.60) return "substantial";
        if (kappa > 0.40) return "moderate";
        if (kappa > 0.20) return "fair";
        return "poor";
    }

    // ---------- bias probes ----------

    /**
     * Verbosity bias: does the judge pass long answers more often than short ones, when humans
     * do not? Returns judgePassRate(long) - judgePassRate(short) minus the same human delta, so
     * a genuine quality difference between long and short answers is subtracted out.
     */
    static double verbosityBias(List<Judgement> judgements, int lengthThreshold) {
        double judgeLong = passRate(judgements, true, lengthThreshold, true);
        double judgeShort = passRate(judgements, true, lengthThreshold, false);
        double humanLong = passRate(judgements, false, lengthThreshold, true);
        double humanShort = passRate(judgements, false, lengthThreshold, false);
        return (judgeLong - judgeShort) - (humanLong - humanShort);
    }

    private static double passRate(List<Judgement> js, boolean useJudge, int threshold, boolean wantLong) {
        int pass = 0, total = 0;
        for (Judgement j : js) {
            boolean isLong = j.responseLength() >= threshold;
            if (isLong != wantLong) {
                continue;
            }
            total++;
            if (useJudge ? j.judgePass() : j.humanPass()) {
                pass++;
            }
        }
        return total == 0 ? 0 : (double) pass / total;
    }

    /**
     * Position bias: pass rate when shown first minus pass rate when shown second.
     * The standard mitigation is to run every comparison both ways and average.
     */
    static double positionBias(List<Judgement> judgements) {
        int firstPass = 0, firstTotal = 0, secondPass = 0, secondTotal = 0;
        for (Judgement j : judgements) {
            if (j.positionShown() == 1) {
                firstTotal++;
                if (j.judgePass()) firstPass++;
            } else {
                secondTotal++;
                if (j.judgePass()) secondPass++;
            }
        }
        double first = firstTotal == 0 ? 0 : (double) firstPass / firstTotal;
        double second = secondTotal == 0 ? 0 : (double) secondPass / secondTotal;
        return first - second;
    }

    // ---------- demo data ----------

    /** A well-calibrated judge: disagrees with humans occasionally, without systematic bias. */
    static List<Judgement> calibratedJudge() {
        return List.of(
                new Judgement("a1", true, true, 300, 1),
                new Judgement("a2", true, true, 80, 2),
                new Judgement("a3", false, false, 250, 1),
                new Judgement("a4", false, false, 60, 2),
                new Judgement("a5", true, true, 90, 1),
                new Judgement("a6", false, false, 320, 2),
                new Judgement("a7", true, true, 200, 1),
                new Judgement("a8", false, true, 100, 2),   // one false pass
                new Judgement("a9", true, true, 70, 1),
                new Judgement("a10", false, false, 280, 2));
    }

    /**
     * A verbosity-biased judge: it passes almost everything long and fails almost everything
     * short, regardless of what the human thought. Raw agreement still looks respectable, which
     * is precisely why raw agreement is not enough.
     */
    static List<Judgement> verbosityBiasedJudge() {
        return List.of(
                new Judgement("b1", true, true, 300, 1),
                new Judgement("b2", true, false, 80, 2),    // good but short -> judge fails it
                new Judgement("b3", false, true, 250, 1),   // bad but long   -> judge passes it
                new Judgement("b4", false, false, 60, 2),
                new Judgement("b5", true, false, 90, 1),    // good but short
                new Judgement("b6", false, true, 320, 2),   // bad but long
                new Judgement("b7", true, true, 200, 1),
                new Judgement("b8", false, false, 100, 2),
                new Judgement("b9", true, false, 70, 1),    // good but short
                new Judgement("b10", false, true, 280, 2)); // bad but long
    }

    static void report(String label, List<Judgement> judgements) {
        Confusion c = confusionOf(judgements);
        double kappa = cohensKappa(c);
        System.out.printf("%s%n", label);
        System.out.printf("  agreement      %.0f%%   (target 85-90%%)%n", c.rawAgreement() * 100);
        System.out.printf("  Cohen's kappa  %.3f  (%s)%n", kappa, kappaVerdict(kappa));
        System.out.printf("  false-pass rate %.0f%%  <- judge waving through what humans rejected%n",
                c.falsePassRate() * 100);
        System.out.printf("  verbosity bias %+.2f%n", verbosityBias(judgements, 150));
        System.out.printf("  position bias  %+.2f%n", positionBias(judgements));
    }

    static void main() {
        int passed = 0, failed = 0;

        List<Judgement> good = calibratedJudge();
        List<Judgement> biased = verbosityBiasedJudge();

        System.out.println("=== Judge calibration against a human-labelled holdout ===\n");
        report("CALIBRATED JUDGE", good);
        System.out.println();
        report("VERBOSITY-BIASED JUDGE", biased);

        System.out.println("\n--- checks ---");

        Confusion cGood = confusionOf(good);
        Confusion cBiased = confusionOf(biased);

        boolean c1 = cGood.rawAgreement() >= 0.85;
        System.out.printf("calibrated judge meets the 85%% bar (%.0f%%)      : %s%n",
                cGood.rawAgreement() * 100, c1);
        if (c1) passed++; else failed++;

        boolean c2 = cohensKappa(cGood) > 0.60;
        System.out.printf("calibrated kappa is substantial (%.3f)          : %s%n",
                cohensKappa(cGood), c2);
        if (c2) passed++; else failed++;

        boolean c3 = cohensKappa(cBiased) < cohensKappa(cGood);
        System.out.printf("biased judge scores worse on kappa (%.3f)       : %s%n",
                cohensKappa(cBiased), c3);
        if (c3) passed++; else failed++;

        // The headline lesson: kappa near zero means the judge is no better than a coin weighted
        // to its own base rate, even though raw agreement is 50% and "sounds" like a real signal.
        boolean c4 = cohensKappa(cBiased) < 0.20;
        System.out.printf("biased judge kappa is 'poor' (%.3f) despite %.0f%% raw agreement : %s%n",
                cohensKappa(cBiased), cBiased.rawAgreement() * 100, c4);
        System.out.println("    ^ raw agreement is inflated by chance; kappa is what exposes it");
        if (c4) passed++; else failed++;

        double vb = verbosityBias(biased, 150);
        boolean c5 = vb > 0.5;
        System.out.printf("verbosity bias detected (%+.2f, well above 0)   : %s%n", vb, c5);
        System.out.println("    ^ the judge rewards LENGTH, not quality — mitigate by controlling");
        System.out.println("      for length in the rubric, or normalising response length first");
        if (c5) passed++; else failed++;

        boolean c6 = Math.abs(verbosityBias(good, 150)) < 0.3;
        System.out.printf("calibrated judge shows little verbosity bias (%+.2f) : %s%n",
                verbosityBias(good, 150), c6);
        if (c6) passed++; else failed++;

        // A judge that never passes a human-rejected item is the safest failure mode.
        boolean c7 = cGood.falsePassRate() < cBiased.falsePassRate();
        System.out.printf("calibrated judge has a lower false-pass rate    : %s%n", c7);
        System.out.println("    ^ a strict judge costs review time; a lenient one ships bad output");
        if (c7) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: calibration distinguishes a trustworthy judge from a biased one."
                : "FAIL: judge calibration mismatch.");
    }
}
