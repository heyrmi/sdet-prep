package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

/**
 * Transactions - commit, rollback, and Savepoint partial rollback (the ACID demo).
 * Teaches: setAutoCommit(false) starts a manual transaction; commit() makes changes durable;
 * rollback() discards a failed transaction so no partial writes survive; a Savepoint lets you
 * undo just part of a transaction while keeping the earlier work. Interview angle: "what happens
 * to the money if the second UPDATE fails mid-transfer?" — rollback leaves the account untouched.
 *
 * Self-contained: models a bank transfer in H2 and asserts atomicity for commit, rollback, and savepoint.
 */
public class Ques3_TransactionsCommitRollbackSavepoint {

    private static final String URL = "jdbc:h2:mem:ques3_tx;DB_CLOSE_DELAY=-1";

    static void createAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE accounts (id INT PRIMARY KEY, owner VARCHAR(50), balance INT NOT NULL)");
            st.execute("INSERT INTO accounts VALUES (1, 'Alice', 100)");
            st.execute("INSERT INTO accounts VALUES (2, 'Bob',   100)");
        }
    }

    static int balance(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    static void addBalance(Connection c, int id, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
            ps.setInt(1, delta);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createAndSeed(c);
            c.setAutoCommit(false);

            // --- 1) Successful transfer, then COMMIT: both changes are durable.
            addBalance(c, 1, -30); // Alice -> Bob, 30
            addBalance(c, 2, +30);
            c.commit();
            if (balance(c, 1) == 70 && balance(c, 2) == 130) {
                System.out.println("PASSED: committed transfer persisted (Alice=70, Bob=130).");
                passed++;
            } else { System.out.println("FAILED: commit did not persist transfer."); failed++; }

            // --- 2) Failed transfer, then ROLLBACK: nothing changes.
            try {
                addBalance(c, 1, -50); // debit Alice
                if (balance(c, 1) < 50) throw new SQLException("insufficient funds"); // simulate a business rule failure
                addBalance(c, 2, +50);
                c.commit();
            } catch (SQLException businessError) {
                c.rollback();
                System.out.println("Rolled back after: " + businessError.getMessage());
            }
            if (balance(c, 1) == 70 && balance(c, 2) == 130) {
                System.out.println("PASSED: rollback discarded the partial debit (balances unchanged).");
                passed++;
            } else { System.out.println("FAILED: rollback left partial changes."); failed++; }

            // --- 3) SAVEPOINT partial rollback: keep the first write, undo the second.
            addBalance(c, 1, +10);            // bonus to Alice (want to KEEP this)
            Savepoint sp = c.setSavepoint("after_bonus");
            addBalance(c, 2, +999);           // erroneous credit (want to UNDO this)
            c.rollback(sp);                   // roll back only to the savepoint
            c.commit();
            if (balance(c, 1) == 80 && balance(c, 2) == 130) {
                System.out.println("PASSED: savepoint kept the bonus (Alice=80) and undid the bad credit (Bob=130).");
                passed++;
            } else { System.out.println("FAILED: savepoint partial rollback incorrect."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: transaction semantics verified."
                : "FAILED: transaction semantics have errors.");
    }
}
