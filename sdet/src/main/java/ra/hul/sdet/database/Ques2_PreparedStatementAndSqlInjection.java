package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * PreparedStatement vs SQL Injection - why parameterized queries are non-negotiable.
 * Teaches: a naive string-concatenated query is exploited with a classic {@code ' OR '1'='1}
 * payload (leaking every row), while a PreparedStatement binds the same payload as a literal
 * value so it matches nothing. Interview angle: "how do you prevent SQL injection?" — bind
 * parameters; never concatenate untrusted input into SQL text.
 *
 * Self-contained: seeds a USERS table in H2 and demonstrates both the exploit and the fix with asserts.
 */
public class Ques2_PreparedStatementAndSqlInjection {

    private static final String URL = "jdbc:h2:mem:ques2_injection;DB_CLOSE_DELAY=-1";

    static void createAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE users (id INT PRIMARY KEY, username VARCHAR(50), secret VARCHAR(100))");
            st.execute("INSERT INTO users VALUES (1, 'alice', 'alice-token')");
            st.execute("INSERT INTO users VALUES (2, 'bob',   'bob-token')");
            st.execute("INSERT INTO users VALUES (3, 'admin', 'super-secret-admin-token')");
        }
    }

    /** VULNERABLE: builds SQL by string concatenation. Never do this in real code. */
    static List<String> lookupVulnerable(Connection c, String username) throws SQLException {
        String sql = "SELECT username, secret FROM users WHERE username = '" + username + "'";
        List<String> rows = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) rows.add(rs.getString("username") + ":" + rs.getString("secret"));
        }
        return rows;
    }

    /** SAFE: binds the input as a parameter so it can only ever be a value, not SQL. */
    static List<String> lookupSafe(Connection c, String username) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT username, secret FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(rs.getString("username") + ":" + rs.getString("secret"));
            }
        }
        return rows;
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        String payload = "' OR '1'='1"; // classic injection: makes the WHERE always true

        try (Connection c = DriverManager.getConnection(URL)) {
            createAndSeed(c);

            // 1) The vulnerable query leaks ALL rows when given the injection payload.
            List<String> leaked = lookupVulnerable(c, payload);
            System.out.println("Vulnerable query with payload leaked " + leaked.size() + " rows: " + leaked);
            if (leaked.size() == 3) {
                System.out.println("PASSED: demonstrated injection — concatenation leaked every row.");
                passed++;
            } else { System.out.println("FAILED: expected the exploit to leak all 3 rows."); failed++; }

            // 2) The SAME payload through a PreparedStatement matches nothing (no user is literally named that).
            List<String> safeInjection = lookupSafe(c, payload);
            System.out.println("Safe query with payload returned " + safeInjection.size() + " rows: " + safeInjection);
            if (safeInjection.isEmpty()) {
                System.out.println("PASSED: PreparedStatement neutralized the injection (0 rows).");
                passed++;
            } else { System.out.println("FAILED: parameterized query should not have leaked data."); failed++; }

            // 3) The safe query still returns the correct single row for a legitimate username.
            List<String> legit = lookupSafe(c, "bob");
            System.out.println("Safe query for 'bob' returned: " + legit);
            if (legit.size() == 1 && legit.getFirst().equals("bob:bob-token")) {
                System.out.println("PASSED: PreparedStatement returned exactly the correct row.");
                passed++;
            } else { System.out.println("FAILED: expected exactly bob's row."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: SQL injection demo verified."
                : "FAILED: SQL injection demo has errors.");
    }
}
