package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Quality Assertion - compare a report/aggregation output to a golden expected dataset.
 * Teaches: the "does the data match expectation" pattern SDETs use on data pipelines — run an
 * aggregation, materialize the result, and assert it equals a test-defined EXPECTED dataset, plus
 * data-quality rule checks (no NULLs in required columns, no negative amounts). Interview angle:
 * "how do you test an ETL/report output?" — assert the whole result set against a golden set and
 * assert invariants, rather than eyeballing a few rows.
 *
 * Self-contained: seeds sales in H2, aggregates revenue per region, and asserts it equals a golden Map.
 */
public class Ques8_DataQualityAssertion {

    private static final String URL = "jdbc:h2:mem:ques8_dq;DB_CLOSE_DELAY=-1";

    static void createAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE sales (
                        id     INT PRIMARY KEY,
                        region VARCHAR(20) NOT NULL,
                        amount INT NOT NULL
                    )
                    """);
            st.execute("""
                    INSERT INTO sales VALUES
                        (1, 'NORTH', 100),
                        (2, 'NORTH', 150),
                        (3, 'SOUTH', 200),
                        (4, 'EAST',   50),
                        (5, 'EAST',   75),
                        (6, 'SOUTH', 300)
                    """);
        }
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createAndSeed(c);

            // --- The "golden" expected dataset the test asserts against.
            Map<String, Integer> expected = new LinkedHashMap<>();
            expected.put("EAST", 125);
            expected.put("NORTH", 250);
            expected.put("SOUTH", 500);

            // --- Actual aggregation output from the "pipeline" (a GROUP BY report).
            Map<String, Integer> actual = new LinkedHashMap<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT region, SUM(amount) AS total FROM sales GROUP BY region ORDER BY region")) {
                while (rs.next()) actual.put(rs.getString("region"), rs.getInt("total"));
            }
            System.out.println("Report output: " + actual);
            System.out.println("Golden expected: " + expected);

            if (actual.equals(expected)) {
                System.out.println("PASSED: aggregated report matches the golden expected dataset exactly.");
                passed++;
            } else { System.out.println("FAILED: report output diverged from golden set."); failed++; }

            // --- Data-quality rule 1: no NULLs in the required 'region' column.
            long nullRegions;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sales WHERE region IS NULL")) {
                rs.next(); nullRegions = rs.getLong(1);
            }
            if (nullRegions == 0) { System.out.println("PASSED: DQ rule — no NULLs in required 'region' column."); passed++; }
            else { System.out.println("FAILED: found " + nullRegions + " NULL regions."); failed++; }

            // --- Data-quality rule 2: no negative amounts.
            long negatives;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sales WHERE amount < 0")) {
                rs.next(); negatives = rs.getLong(1);
            }
            if (negatives == 0) { System.out.println("PASSED: DQ rule — no negative amounts."); passed++; }
            else { System.out.println("FAILED: found " + negatives + " negative amounts."); failed++; }

            // --- Reconciliation: sum of the per-region report equals the grand total of raw rows.
            long grandTotal;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT SUM(amount) FROM sales")) {
                rs.next(); grandTotal = rs.getLong(1);
            }
            long reportTotal = actual.values().stream().mapToLong(Integer::longValue).sum();
            System.out.println("grandTotal=" + grandTotal + " reportTotal=" + reportTotal);
            if (grandTotal == reportTotal && grandTotal == 875) {
                System.out.println("PASSED: report totals reconcile with raw data (875).");
                passed++;
            } else { System.out.println("FAILED: totals do not reconcile."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: data quality assertions verified."
                : "FAILED: data quality assertions have errors.");
    }
}
