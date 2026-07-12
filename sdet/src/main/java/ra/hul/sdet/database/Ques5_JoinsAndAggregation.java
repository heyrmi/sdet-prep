package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Joins & Aggregation - INNER/LEFT JOIN, GROUP BY ... HAVING, and COUNT/SUM/AVG.
 * Teaches: joining related tables, aggregating with GROUP BY, filtering groups with HAVING, and the
 * difference between INNER (only matches) and LEFT (keep-all-left) joins. Interview angle: "how many
 * employees per department, and which departments have more than one?" — GROUP BY dept, HAVING COUNT>1.
 *
 * Self-contained: seeds departments + employees in H2 and asserts each query against hand-computed values.
 */
public class Ques5_JoinsAndAggregation {

    private static final String URL = "jdbc:h2:mem:ques5_joins;DB_CLOSE_DELAY=-1";

    static void createAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE departments (id INT PRIMARY KEY, name VARCHAR(50))");
            st.execute("CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(50), dept_id INT, salary INT)");

            st.execute("INSERT INTO departments VALUES (10, 'Engineering'), (20, 'Sales'), (30, 'Marketing')");
            // Marketing (30) has no employees -> exercises LEFT JOIN. dept_id 40 does not exist.
            st.execute("""
                    INSERT INTO employees VALUES
                        (1, 'Alice',  10, 120000),
                        (2, 'Bob',    10, 100000),
                        (3, 'Carol',  10, 140000),
                        (4, 'Dave',   20,  90000),
                        (5, 'Erin',   20,  95000),
                        (6, 'Frank',  40,  70000)
                    """);
        }
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createAndSeed(c);
            try (Statement st = c.createStatement()) {

                // 1) INNER JOIN: only employees whose dept_id matches a real department (Frank@40 excluded).
                try (ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM employees e JOIN departments d ON e.dept_id = d.id")) {
                    rs.next();
                    int matched = rs.getInt(1);
                    System.out.println("INNER JOIN matched employees: " + matched);
                    if (matched == 5) { System.out.println("PASSED: INNER JOIN dropped the orphan employee (5 of 6)."); passed++; }
                    else { System.out.println("FAILED: expected 5 joined rows."); failed++; }
                }

                // 2) LEFT JOIN + GROUP BY: headcount per department, including the empty Marketing dept.
                Map<String, Integer> headcount = new LinkedHashMap<>();
                try (ResultSet rs = st.executeQuery("""
                        SELECT d.name, COUNT(e.id) AS cnt
                        FROM departments d
                        LEFT JOIN employees e ON e.dept_id = d.id
                        GROUP BY d.name
                        ORDER BY d.name
                        """)) {
                    while (rs.next()) headcount.put(rs.getString("name"), rs.getInt("cnt"));
                }
                System.out.println("Headcount (LEFT JOIN): " + headcount);
                if (headcount.equals(Map.of("Engineering", 3, "Sales", 2, "Marketing", 0))) {
                    System.out.println("PASSED: LEFT JOIN kept Marketing with headcount 0.");
                    passed++;
                } else { System.out.println("FAILED: headcount per department mismatch."); failed++; }

                // 3) GROUP BY ... HAVING: departments with average salary above 100k.
                try (ResultSet rs = st.executeQuery("""
                        SELECT d.name, AVG(e.salary) AS avg_sal
                        FROM departments d
                        JOIN employees e ON e.dept_id = d.id
                        GROUP BY d.name
                        HAVING AVG(e.salary) > 100000
                        """)) {
                    rs.next();
                    String dept = rs.getString("name");
                    int avg = rs.getInt("avg_sal");
                    boolean onlyOne = !rs.next();
                    System.out.println("Dept avg salary > 100k: " + dept + " = " + avg);
                    // Engineering avg = (120000+100000+140000)/3 = 120000; Sales avg = 92500 (excluded).
                    if (onlyOne && dept.equals("Engineering") && avg == 120000) {
                        System.out.println("PASSED: HAVING selected only Engineering (avg 120000).");
                        passed++;
                    } else { System.out.println("FAILED: HAVING result mismatch."); failed++; }
                }

                // 4) Aggregates SUM/AVG over all matched employees.
                try (ResultSet rs = st.executeQuery("""
                        SELECT SUM(e.salary) AS total, AVG(e.salary) AS avg_all
                        FROM employees e JOIN departments d ON e.dept_id = d.id
                        """)) {
                    rs.next();
                    long total = rs.getLong("total");
                    int avgAll = rs.getInt("avg_all");
                    // 120000+100000+140000+90000+95000 = 545000; avg = 109000.
                    System.out.println("SUM=" + total + " AVG=" + avgAll);
                    if (total == 545000 && avgAll == 109000) {
                        System.out.println("PASSED: SUM/AVG match hand-computed values.");
                        passed++;
                    } else { System.out.println("FAILED: SUM/AVG mismatch."); failed++; }
                }
            }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: joins & aggregation verified."
                : "FAILED: joins & aggregation have errors.");
    }
}
