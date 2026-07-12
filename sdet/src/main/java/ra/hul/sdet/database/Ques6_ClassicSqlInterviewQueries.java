package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classic SQL interview queries - Nth-highest salary, find duplicates, department-wise top earner.
 * Teaches: window functions (ROW_NUMBER, DENSE_RANK, RANK OVER PARTITION BY) and the older
 * correlated-subquery technique, plus GROUP BY ... HAVING COUNT>1 for duplicate detection. Interview
 * angle: "find the 2nd-highest salary" is a rite-of-passage question — know both the window-function
 * and the correlated-subquery answers, and know why DENSE_RANK handles ties.
 *
 * Self-contained: seeds employees in H2 and asserts each classic query against hand-computed values.
 */
public class Ques6_ClassicSqlInterviewQueries {

    private static final String URL = "jdbc:h2:mem:ques6_classic;DB_CLOSE_DELAY=-1";

    static void createAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(50), dept VARCHAR(30), salary INT)");
            // Distinct salaries: 140k, 120k, 100k, 90k, 80k. 120k appears twice (Bob & Grace) -> a duplicate salary.
            st.execute("""
                    INSERT INTO employees VALUES
                        (1, 'Alice', 'Engineering', 140000),
                        (2, 'Bob',   'Engineering', 120000),
                        (3, 'Carol', 'Engineering', 100000),
                        (4, 'Dave',  'Sales',        90000),
                        (5, 'Erin',  'Sales',        80000),
                        (6, 'Frank', 'Sales',       120000),
                        (7, 'Grace', 'Marketing',   120000)
                    """);
        }
    }

    /** Nth-highest DISTINCT salary via DENSE_RANK (handles ties: all 120k share rank). */
    static Integer nthHighestWindow(Connection c, int n) throws SQLException {
        String sql = """
                SELECT salary FROM (
                    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
                    FROM employees
                ) WHERE rnk = ?
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /** Nth-highest DISTINCT salary via a correlated subquery (classic, no window functions). */
    static Integer nthHighestCorrelated(Connection c, int n) throws SQLException {
        String sql = """
                SELECT DISTINCT e.salary
                FROM employees e
                WHERE (n - 1) = (
                    SELECT COUNT(DISTINCT e2.salary)
                    FROM employees e2
                    WHERE e2.salary > e.salary
                )
                """.replace("n - 1", (n - 1) + "");
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createAndSeed(c);

            // Distinct salary ranking (DESC): 1=140k, 2=120k, 3=100k, 4=90k, 5=80k.
            // 1) 2nd-highest via window function.
            Integer w2 = nthHighestWindow(c, 2);
            System.out.println("2nd-highest (DENSE_RANK): " + w2);
            if (w2 != null && w2 == 120000) { System.out.println("PASSED: DENSE_RANK 2nd-highest = 120000."); passed++; }
            else { System.out.println("FAILED: window 2nd-highest wrong."); failed++; }

            // 2) 3rd-highest via correlated subquery.
            Integer cs3 = nthHighestCorrelated(c, 3);
            System.out.println("3rd-highest (correlated subquery): " + cs3);
            if (cs3 != null && cs3 == 100000) { System.out.println("PASSED: correlated 3rd-highest = 100000."); passed++; }
            else { System.out.println("FAILED: correlated 3rd-highest wrong."); failed++; }

            // Both techniques agree for the same N.
            if (nthHighestWindow(c, 3).equals(nthHighestCorrelated(c, 3))) {
                System.out.println("PASSED: window and correlated-subquery agree for N=3.");
                passed++;
            } else { System.out.println("FAILED: techniques disagree."); failed++; }

            // 3) Find duplicate salaries (GROUP BY ... HAVING COUNT > 1). Only 120000 (x3).
            List<Integer> dups = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT salary FROM employees GROUP BY salary HAVING COUNT(*) > 1 ORDER BY salary")) {
                while (rs.next()) dups.add(rs.getInt(1));
            }
            System.out.println("Duplicate salaries: " + dups);
            if (dups.equals(List.of(120000))) { System.out.println("PASSED: duplicate salary detected (120000 appears 3x)."); passed++; }
            else { System.out.println("FAILED: duplicate detection wrong."); failed++; }

            // 4) Department-wise top earner via RANK() OVER (PARTITION BY dept).
            Map<String, String> topEarner = new LinkedHashMap<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("""
                         SELECT dept, name, salary FROM (
                             SELECT dept, name, salary,
                                    RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rnk
                             FROM employees
                         ) WHERE rnk = 1
                         ORDER BY dept
                         """)) {
                while (rs.next()) topEarner.put(rs.getString("dept"), rs.getString("name"));
            }
            System.out.println("Top earner per dept: " + topEarner);
            // Engineering -> Alice(140k), Sales -> Frank(120k), Marketing -> Grace(120k).
            if (topEarner.equals(Map.of("Engineering", "Alice", "Sales", "Frank", "Marketing", "Grace"))) {
                System.out.println("PASSED: PARTITION BY found each department's top earner.");
                passed++;
            } else { System.out.println("FAILED: department-wise top earner wrong."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: classic SQL queries verified."
                : "FAILED: classic SQL queries have errors.");
    }
}
