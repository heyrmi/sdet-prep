package ra.hul.sdet.propertytesting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Mutation Testing - measuring whether your tests would actually notice a bug.
 *
 * <p>Line coverage answers "did this line run?". That is almost never the question you care
 * about. A test suite can execute 100% of the lines and assert nothing meaningful about any of
 * them — and a coverage badge will proudly say 100%.
 *
 * <p>Mutation testing asks the real question: <b>if I deliberately break the code, does any test
 * fail?</b> Each deliberate break is a <i>mutant</i>. A mutant your tests catch is <i>killed</i>.
 * One they miss <i>survives</i>, and every survivor is a concrete, specific gap:
 *
 * <pre>
 *   mutation score = killed / total mutants
 * </pre>
 *
 * <p>Common mutation operators, all represented below:
 * <ul>
 *   <li><b>Conditionals boundary</b> — {@code <} becomes {@code <=}, the classic off-by-one</li>
 *   <li><b>Negate conditionals</b> — {@code ==} becomes {@code !=}</li>
 *   <li><b>Math</b> — {@code +} becomes {@code -}</li>
 *   <li><b>Return values</b> — return a constant instead of the computed value</li>
 *   <li><b>Void method calls</b> — remove the call entirely</li>
 * </ul>
 *
 * <p>Interview angle: "we have 90% code coverage" is a statement about execution, not about
 * quality. Mutation score is the metric that means something, and knowing the difference —
 * plus knowing that mutation testing is slow and belongs on critical code, not everywhere — is
 * the judgment being assessed.
 *
 * <p>This problem implements a miniature mutation tester over a small function so the mechanic
 * is visible end to end. The real tool for the repo is pitest — see
 * {@code verifier/pom.xml} and {@code mvn -pl verifier test -Pmutation}.
 *
 * <p>Self-contained: no network, fully deterministic.
 */
public class Ques4_MutationTesting {

    /**
     * The function under test: classify an order total into a shipping band.
     *
     * <pre>
     *   total &lt;  0      -> "INVALID"
     *   total &lt; 50      -> "STANDARD"
     *   total &lt; 200     -> "EXPRESS"
     *   otherwise       -> "FREE"
     * </pre>
     */
    static String shippingBand(int total) {
        if (total < 0) {
            return "INVALID";
        }
        if (total < 50) {
            return "STANDARD";
        }
        if (total < 200) {
            return "EXPRESS";
        }
        return "FREE";
    }

    /** A mutant is a deliberately broken variant of the function, with a label. */
    record Mutant(String operator, String description, Function<Integer, String> mutated) {
    }

    /**
     * The mutants. In a real tool these are generated from bytecode; here they are written out
     * so you can see exactly what each operator does.
     */
    static List<Mutant> mutants() {
        return List.of(
                new Mutant("conditionals-boundary", "total < 0  ->  total <= 0", total -> {
                    if (total <= 0) return "INVALID";
                    if (total < 50) return "STANDARD";
                    if (total < 200) return "EXPRESS";
                    return "FREE";
                }),
                new Mutant("conditionals-boundary", "total < 50  ->  total <= 50", total -> {
                    if (total < 0) return "INVALID";
                    if (total <= 50) return "STANDARD";
                    if (total < 200) return "EXPRESS";
                    return "FREE";
                }),
                new Mutant("conditionals-boundary", "total < 200  ->  total <= 200", total -> {
                    if (total < 0) return "INVALID";
                    if (total < 50) return "STANDARD";
                    if (total <= 200) return "EXPRESS";
                    return "FREE";
                }),
                new Mutant("negate-conditional", "total < 50  ->  total >= 50", total -> {
                    if (total < 0) return "INVALID";
                    if (total >= 50) return "STANDARD";
                    if (total < 200) return "EXPRESS";
                    return "FREE";
                }),
                new Mutant("math", "boundary 50  ->  boundary 49", total -> {
                    if (total < 0) return "INVALID";
                    if (total < 49) return "STANDARD";
                    if (total < 200) return "EXPRESS";
                    return "FREE";
                }),
                new Mutant("return-value", "the INVALID branch returns STANDARD", total -> {
                    if (total < 0) return "STANDARD";
                    if (total < 50) return "STANDARD";
                    if (total < 200) return "EXPRESS";
                    return "FREE";
                }),
                new Mutant("remove-branch", "the FREE band is never reached", total -> {
                    if (total < 0) return "INVALID";
                    if (total < 50) return "STANDARD";
                    return "EXPRESS";
                }));
    }

    /** A test suite is just a list of named assertions over the function. */
    record TestCase(String name, java.util.function.Predicate<Function<Integer, String>> check) {
    }

    /**
     * A suite that achieves 100% LINE COVERAGE — every branch is executed — while asserting only
     * mid-band values. This is the shape of an enormous amount of real-world test code.
     */
    static List<TestCase> coverageOnlySuite() {
        return List.of(
                new TestCase("negative is INVALID", f -> f.apply(-10).equals("INVALID")),
                new TestCase("25 is STANDARD", f -> f.apply(25).equals("STANDARD")),
                new TestCase("100 is EXPRESS", f -> f.apply(100).equals("EXPRESS")),
                new TestCase("500 is FREE", f -> f.apply(500).equals("FREE")));
    }

    /** The same suite plus boundary cases — the ones that actually pin the behaviour down. */
    static List<TestCase> boundarySuite() {
        List<TestCase> suite = new ArrayList<>(coverageOnlySuite());
        suite.add(new TestCase("0 is STANDARD (not INVALID)", f -> f.apply(0).equals("STANDARD")));
        suite.add(new TestCase("49 is STANDARD", f -> f.apply(49).equals("STANDARD")));
        suite.add(new TestCase("50 is EXPRESS", f -> f.apply(50).equals("EXPRESS")));
        suite.add(new TestCase("199 is EXPRESS", f -> f.apply(199).equals("EXPRESS")));
        suite.add(new TestCase("200 is FREE", f -> f.apply(200).equals("FREE")));
        return suite;
    }

    record MutationReport(int killed, int survived, List<Mutant> survivors) {
        double score() {
            int total = killed + survived;
            return total == 0 ? 1.0 : (double) killed / total;
        }
    }

    /** Runs the suite against every mutant. A mutant is killed if any test fails on it. */
    static MutationReport run(List<TestCase> suite, List<Mutant> mutants) {
        int killed = 0;
        List<Mutant> survivors = new ArrayList<>();

        for (Mutant m : mutants) {
            boolean caught = false;
            for (TestCase t : suite) {
                boolean holds;
                try {
                    holds = t.check().test(m.mutated());
                } catch (RuntimeException e) {
                    holds = false;   // an exception counts as a failing test, so the mutant dies
                }
                if (!holds) {
                    caught = true;
                    break;
                }
            }
            if (caught) {
                killed++;
            } else {
                survivors.add(m);
            }
        }
        return new MutationReport(killed, survivors.size(), survivors);
    }

    /** Sanity check: the suite must pass against the UNMUTATED function. */
    static boolean suitePassesOnOriginal(List<TestCase> suite) {
        for (TestCase t : suite) {
            if (!t.check().test(Ques4_MutationTesting::shippingBand)) {
                return false;
            }
        }
        return true;
    }

    static void main() {
        int passed = 0, failed = 0;
        List<Mutant> mutants = mutants();

        System.out.println("=== Mutation testing: " + mutants.size() + " mutants ===\n");

        List<TestCase> weak = coverageOnlySuite();
        List<TestCase> strong = boundarySuite();

        MutationReport weakReport = run(weak, mutants);
        MutationReport strongReport = run(strong, mutants);

        System.out.printf("SUITE A (%d tests, 100%% line coverage, mid-band values only)%n", weak.size());
        System.out.printf("  killed %d/%d   mutation score %.0f%%%n",
                weakReport.killed(), mutants.size(), weakReport.score() * 100);
        for (Mutant s : weakReport.survivors()) {
            System.out.printf("    SURVIVED [%s] %s%n", s.operator(), s.description());
        }

        System.out.printf("%nSUITE B (%d tests, same coverage, plus boundary values)%n", strong.size());
        System.out.printf("  killed %d/%d   mutation score %.0f%%%n",
                strongReport.killed(), mutants.size(), strongReport.score() * 100);
        for (Mutant s : strongReport.survivors()) {
            System.out.printf("    SURVIVED [%s] %s%n", s.operator(), s.description());
        }

        System.out.println("\n--- checks ---");

        // 1. Both suites must be valid tests of the real function.
        boolean c1 = suitePassesOnOriginal(weak) && suitePassesOnOriginal(strong);
        System.out.println("both suites pass against the real function  : " + c1);
        if (c1) passed++; else failed++;

        // 2. THE point: identical line coverage, very different mutation scores.
        boolean c2 = weakReport.score() < strongReport.score();
        System.out.printf("suite B scores higher than suite A (%.0f%% vs %.0f%%) : %s%n",
                strongReport.score() * 100, weakReport.score() * 100, c2);
        System.out.println("    ^ both suites execute EVERY line and branch. Line coverage");
        System.out.println("      cannot tell them apart; mutation score can.");
        if (c2) passed++; else failed++;

        // 3. The weak suite leaves boundary mutants alive.
        boolean c3 = weakReport.survivors().stream()
                .anyMatch(m -> m.operator().equals("conditionals-boundary"));
        System.out.println("boundary mutants survive the weak suite     : " + c3);
        System.out.println("    ^ every survivor names a missing test case, precisely");
        if (c3) passed++; else failed++;

        // 4. The boundary suite kills everything.
        boolean c4 = strongReport.survived() == 0;
        System.out.printf("boundary suite kills all %d mutants          : %s%n", mutants.size(), c4);
        if (c4) passed++; else failed++;

        // 5. Obvious mutants die even under a weak suite — mutation testing is not all-or-nothing.
        boolean c5 = weakReport.killed() > 0;
        System.out.printf("even the weak suite kills %d mutants         : %s%n",
                weakReport.killed(), c5);
        if (c5) passed++; else failed++;

        System.out.println("\n--- how to use this for real ---");
        System.out.println("  * Mutation testing is SLOW — it reruns the suite once per mutant.");
        System.out.println("    Scope it to critical code (payments, auth, pricing), not everywhere.");
        System.out.println("  * Watch for EQUIVALENT mutants: changes that cannot alter behaviour");
        System.out.println("    and so can never be killed. They cap your achievable score below");
        System.out.println("    100%, and chasing them is wasted effort.");
        System.out.println("  * Measure the baseline before gating on it. A threshold set above");
        System.out.println("    what the code can reach just teaches people to disable the check.");
        System.out.println("  * In this repo: mvn -pl verifier test -Pmutation");

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: mutation score distinguishes suites that line coverage cannot."
                : "FAIL: mutation testing mismatch.");
    }
}
