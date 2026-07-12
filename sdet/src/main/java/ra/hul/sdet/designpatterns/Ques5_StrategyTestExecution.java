package ra.hul.sdet.designpatterns;

import java.util.List;
import java.util.Set;

/**
 * Strategy Pattern for Test Execution - swap the "which tests + how" algorithm at runtime.
 * Common SDET question: "Implement smoke/regression/full run strategies behind one interface."
 *
 * Self-contained. A context (TestRunner) delegates to whichever TestStrategy is selected via config.
 * Run main() — no browser/network needed.
 */
public class Ques5_StrategyTestExecution {

    /** A test case tagged with its suite groups (like TestNG @Test(groups=...)). */
    record TestCase(String name, Set<String> tags) {}

    /** Strategy contract: decide which tests run and report how many. */
    interface TestStrategy {
        String name();
        List<TestCase> select(List<TestCase> all);
    }

    static final class SmokeStrategy implements TestStrategy {
        public String name() { return "SMOKE"; }
        public List<TestCase> select(List<TestCase> all) {
            return all.stream().filter(t -> t.tags().contains("smoke")).toList();
        }
    }

    static final class RegressionStrategy implements TestStrategy {
        public String name() { return "REGRESSION"; }
        public List<TestCase> select(List<TestCase> all) {
            return all.stream().filter(t -> t.tags().contains("regression")).toList();
        }
    }

    static final class FullStrategy implements TestStrategy {
        public String name() { return "FULL"; }
        public List<TestCase> select(List<TestCase> all) { return all; }
    }

    /** Context: holds the active strategy and delegates. Strategy is chosen at runtime. */
    static final class TestRunner {
        private TestStrategy strategy;
        TestRunner(TestStrategy s) { this.strategy = s; }
        void setStrategy(TestStrategy s) { this.strategy = s; }
        int run(List<TestCase> all) {
            List<TestCase> selected = strategy.select(all);
            System.out.println("  [" + strategy.name() + "] running " + selected.size() + " tests: "
                    + selected.stream().map(TestCase::name).toList());
            return selected.size();
        }
    }

    static TestStrategy fromConfig(String cfg) {
        return switch (cfg.toLowerCase()) {
            case "smoke" -> new SmokeStrategy();
            case "regression" -> new RegressionStrategy();
            default -> new FullStrategy();
        };
    }

    static void main() {
        List<TestCase> suite = List.of(
                new TestCase("login",    Set.of("smoke", "regression")),
                new TestCase("checkout", Set.of("regression")),
                new TestCase("search",   Set.of("smoke")),
                new TestCase("reports",  Set.of("regression")),
                new TestCase("i18n",     Set.of("full"))
        );

        TestRunner runner = new TestRunner(fromConfig("smoke"));
        int smoke = runner.run(suite);

        runner.setStrategy(fromConfig("regression")); // swap at runtime
        int regression = runner.run(suite);

        runner.setStrategy(fromConfig("full"));
        int full = runner.run(suite);

        boolean ok = smoke == 2 && regression == 3 && full == suite.size();
        System.out.println(ok ? "PASSED: each strategy selected the expected tests."
                              : "FAILED: strategy selection unexpected (" + smoke + "/" + regression + "/" + full + ").");
    }
}
