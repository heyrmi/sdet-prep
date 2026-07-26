package ra.hul.sdet.aiqa;

import java.util.ArrayList;
import java.util.List;

/**
 * Eval Regression Gate - blocking a merge on an eval score without creating a flaky gate.
 *
 * <p>The gate is what turns evaluation from a dashboard into engineering: a prompt change runs the
 * golden dataset, and a regression below baseline blocks the merge — exactly like a test suite.
 *
 * <p>The trap is that a naive gate ({@code if (score < baseline) fail}) is a FLAKY TEST. Sampling
 * variance alone moves the score run to run, so the gate fires on noise, developers learn to
 * re-run it, and within a fortnight someone disables it. That is the same failure curve as any
 * flaky test, and it has the same remedy: <b>measure the noise, then set the threshold outside
 * it.</b>
 *
 * <p>This problem implements:
 * <ul>
 *   <li>a tolerance band derived from the observed standard deviation, not guessed;</li>
 *   <li>a minimum-sample requirement, so a 5-case run cannot block a merge;</li>
 *   <li>per-tag gating, because an overall pass rate hides a collapse in the high-stakes slice;</li>
 *   <li>a distinction between a REGRESSION (block) and a DRIFT WARNING (report).</li>
 * </ul>
 *
 * <p>Interview angle: "how do you stop the eval gate becoming the thing everyone ignores?" —
 * tolerance from measured variance, plus a separate warn tier.
 *
 * <p>Self-contained: fixed score samples, no model calls.
 */
public class Ques6_EvalRegressionGate {

    /** One eval run's result on a slice of the golden dataset. */
    record RunScore(String tag, double passRate, int sampleSize) {
    }

    /** Baseline statistics gathered from repeated runs of the SAME code. */
    record Baseline(String tag, double mean, double stdDev, int runs) {
    }

    enum Outcome { PASS, WARN, BLOCK, INSUFFICIENT_DATA }

    record GateResult(String tag, Outcome outcome, double delta, double threshold, String reason) {
    }

    /** Gate configuration. */
    record GateConfig(
            // sigmaMultiplier x observed stdDev is the tolerance band. 2 sigma leaves roughly a
            // 2.5% one-sided false-alarm rate, which is about the most a merge gate can carry.
            double sigmaMultiplier,
            // warnMultiplier is the tighter inner band: report it, do not block on it.
            double warnMultiplier,
            // minSampleSize below which the run cannot block anything.
            int minSampleSize,
            // absoluteFloor blocks regardless of variance — a hard product requirement.
            double absoluteFloor) {

        static GateConfig standard() {
            return new GateConfig(2.0, 1.0, 30, 0.70);
        }
    }

    /** Mean of a sample. */
    static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    /** Sample standard deviation (Bessel-corrected: n-1). */
    static double stdDev(List<Double> values) {
        if (values.size() < 2) {
            return 0;
        }
        double m = mean(values);
        double sumSq = 0;
        for (double v : values) {
            sumSq += (v - m) * (v - m);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    /**
     * Establishes a baseline by running the SAME configuration repeatedly. This is the step teams
     * skip, and skipping it is why their gate is flaky: you cannot tell signal from noise until
     * you have measured the noise.
     */
    static Baseline establishBaseline(String tag, List<Double> repeatedRuns) {
        return new Baseline(tag, mean(repeatedRuns), stdDev(repeatedRuns), repeatedRuns.size());
    }

    /** Evaluates one slice against its baseline. */
    static GateResult evaluate(RunScore run, Baseline baseline, GateConfig cfg) {
        double delta = run.passRate() - baseline.mean();

        if (run.sampleSize() < cfg.minSampleSize()) {
            // A tiny sample can swing wildly. Reporting it as a regression is noise.
            return new GateResult(run.tag(), Outcome.INSUFFICIENT_DATA, delta, 0,
                    "sample size " + run.sampleSize() + " below minimum " + cfg.minSampleSize());
        }

        if (run.passRate() < cfg.absoluteFloor()) {
            return new GateResult(run.tag(), Outcome.BLOCK, delta, cfg.absoluteFloor(),
                    String.format("below the absolute floor of %.0f%%", cfg.absoluteFloor() * 100));
        }

        double blockBand = cfg.sigmaMultiplier() * baseline.stdDev();
        double warnBand = cfg.warnMultiplier() * baseline.stdDev();

        if (delta < -blockBand) {
            return new GateResult(run.tag(), Outcome.BLOCK, delta, blockBand,
                    String.format("regression of %.1f pts exceeds the %.1f-pt tolerance (%.1f sigma)",
                            -delta * 100, blockBand * 100, cfg.sigmaMultiplier()));
        }
        if (delta < -warnBand) {
            return new GateResult(run.tag(), Outcome.WARN, delta, warnBand,
                    String.format("drift of %.1f pts is within tolerance but worth watching",
                            -delta * 100));
        }
        return new GateResult(run.tag(), Outcome.PASS, delta, blockBand, "within tolerance");
    }

    /** Runs the gate over every slice; any BLOCK fails the merge. */
    static List<GateResult> evaluateAll(List<RunScore> runs, List<Baseline> baselines, GateConfig cfg) {
        List<GateResult> results = new ArrayList<>();
        for (RunScore run : runs) {
            Baseline b = baselines.stream()
                    .filter(x -> x.tag().equals(run.tag()))
                    .findFirst()
                    .orElse(new Baseline(run.tag(), run.passRate(), 0, 0));
            results.add(evaluate(run, b, cfg));
        }
        return results;
    }

    static boolean merged(List<GateResult> results) {
        return results.stream().noneMatch(r -> r.outcome() == Outcome.BLOCK);
    }

    static void main() {
        int passed = 0, failed = 0;
        GateConfig cfg = GateConfig.standard();

        // Ten runs of identical code. The spread here IS the noise floor.
        List<Double> repeated = List.of(0.86, 0.88, 0.85, 0.87, 0.89, 0.86, 0.88, 0.87, 0.85, 0.89);
        Baseline overall = establishBaseline("overall", repeated);

        List<Double> highStakesRepeated = List.of(0.92, 0.94, 0.93, 0.92, 0.94, 0.93, 0.95, 0.92);
        Baseline highStakes = establishBaseline("high-stakes", highStakesRepeated);

        System.out.println("=== Baselines from repeated runs of UNCHANGED code ===");
        System.out.printf("  overall      mean %.3f  stdDev %.4f  (%d runs)%n",
                overall.mean(), overall.stdDev(), overall.runs());
        System.out.printf("  high-stakes  mean %.3f  stdDev %.4f  (%d runs)%n",
                highStakes.mean(), highStakes.stdDev(), highStakes.runs());
        System.out.printf("  => block band = 2 sigma = %.1f pts overall%n", overall.stdDev() * 2 * 100);
        System.out.println("     Anything inside that band is indistinguishable from noise.\n");

        List<Baseline> baselines = List.of(overall, highStakes);

        System.out.println("--- checks ---");

        // 1. A run inside the noise band must NOT block. This is the anti-flake requirement.
        List<GateResult> noise = evaluateAll(
                List.of(new RunScore("overall", 0.855, 200), new RunScore("high-stakes", 0.93, 100)),
                baselines, cfg);
        boolean c1 = merged(noise);
        System.out.printf("run at 85.5%% (inside noise band) merges          : %s%n", c1);
        System.out.println("    ^ a gate that fires on sampling variance gets disabled within weeks");
        if (c1) passed++; else failed++;

        // 2. A genuine regression outside the band must block.
        List<GateResult> regression = evaluateAll(
                List.of(new RunScore("overall", 0.79, 200), new RunScore("high-stakes", 0.93, 100)),
                baselines, cfg);
        boolean c2 = !merged(regression);
        System.out.printf("run at 79%% (well outside band) blocks            : %s%n", c2);
        if (c2) passed++; else failed++;

        // 3. Drift is reported without blocking.
        List<GateResult> drift = evaluateAll(
                List.of(new RunScore("overall", 0.851, 200)), baselines, cfg);
        boolean c3 = drift.get(0).outcome() == Outcome.WARN && merged(drift);
        System.out.printf("borderline run warns but still merges            : %s (%s)%n",
                c3, drift.get(0).outcome());
        if (c3) passed++; else failed++;

        // 4. Per-tag gating: overall looks fine, high-stakes collapsed. Must block.
        List<GateResult> sliceCollapse = evaluateAll(
                List.of(new RunScore("overall", 0.87, 200), new RunScore("high-stakes", 0.72, 100)),
                baselines, cfg);
        boolean c4 = !merged(sliceCollapse);
        System.out.printf("overall healthy but high-stakes collapsed blocks : %s%n", c4);
        System.out.println("    ^ an aggregate score hides exactly the regressions you care about most");
        if (c4) passed++; else failed++;

        // 5. Small samples cannot block.
        List<GateResult> smallSample = evaluateAll(
                List.of(new RunScore("overall", 0.40, 5)), baselines, cfg);
        boolean c5 = smallSample.get(0).outcome() == Outcome.INSUFFICIENT_DATA && merged(smallSample);
        System.out.printf("5-case run at 40%% cannot block (insufficient)    : %s%n", c5);
        if (c5) passed++; else failed++;

        // 6. The absolute floor blocks regardless of what the baseline says.
        Baseline lowBaseline = new Baseline("overall", 0.65, 0.05, 10);
        GateResult floored = evaluate(new RunScore("overall", 0.66, 200), lowBaseline, cfg);
        boolean c6 = floored.outcome() == Outcome.BLOCK;
        System.out.printf("66%% blocks on the absolute floor even though it   %n"
                + "  BEATS its own 65%% baseline                     : %s%n", c6);
        System.out.println("    ^ 'better than yesterday' is not the same as 'good enough to ship'");
        if (c6) passed++; else failed++;

        // 7. An improvement obviously merges.
        List<GateResult> better = evaluateAll(
                List.of(new RunScore("overall", 0.94, 200)), baselines, cfg);
        boolean c7 = merged(better) && better.get(0).outcome() == Outcome.PASS;
        System.out.printf("improvement to 94%% merges cleanly               : %s%n", c7);
        if (c7) passed++; else failed++;

        System.out.println("\n--- sample gate output on the regression run ---");
        for (GateResult r : regression) {
            System.out.printf("  [%-17s] %-12s delta %+.1f pts  %s%n",
                    r.outcome(), r.tag(), r.delta() * 100, r.reason());
        }

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: gate blocks real regressions without firing on sampling noise."
                : "FAIL: regression gate mismatch.");
    }
}
