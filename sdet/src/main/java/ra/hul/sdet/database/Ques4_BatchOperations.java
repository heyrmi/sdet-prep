package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Batch Operations - addBatch/executeBatch for bulk inserts vs row-by-row.
 * Teaches: batching groups many INSERTs into one round-trip to the database, which is dramatically
 * faster than executing each statement individually. Interview angle: "you need to load a million
 * rows — how?" — batch inserts (with autoCommit off) instead of a network round-trip per row.
 *
 * Self-contained: inserts the same volume row-by-row and via a batch into H2, asserts equal counts,
 * and prints a (non-asserted) timing comparison so the run stays deterministic.
 */
public class Ques4_BatchOperations {

    private static final String URL = "jdbc:h2:mem:ques4_batch;DB_CLOSE_DELAY=-1";
    private static final int N = 2000;

    static void createTable(Connection c, String name) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE " + name + " (id INT PRIMARY KEY, val VARCHAR(20))");
        }
    }

    static long insertRowByRow(Connection c) throws SQLException {
        long start = System.nanoTime();
        c.setAutoCommit(false);
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO row_by_row (id, val) VALUES (?, ?)")) {
            for (int i = 1; i <= N; i++) {
                ps.setInt(1, i);
                ps.setString(2, "v" + i);
                ps.executeUpdate();
            }
        }
        c.commit();
        c.setAutoCommit(true);
        return System.nanoTime() - start;
    }

    static long insertBatched(Connection c) throws SQLException {
        long start = System.nanoTime();
        c.setAutoCommit(false);
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO batched (id, val) VALUES (?, ?)")) {
            for (int i = 1; i <= N; i++) {
                ps.setInt(1, i);
                ps.setString(2, "v" + i);
                ps.addBatch();
                if (i % 500 == 0) ps.executeBatch(); // flush in chunks to bound memory
            }
            ps.executeBatch(); // flush the remainder
        }
        c.commit();
        c.setAutoCommit(true);
        return System.nanoTime() - start;
    }

    static long count(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createTable(c, "row_by_row");
            createTable(c, "batched");

            long rbrNanos = insertRowByRow(c);
            long batchNanos = insertBatched(c);

            long rbrCount = count(c, "row_by_row");
            long batchCount = count(c, "batched");
            System.out.printf("Row-by-row: %d rows in %.1f ms%n", rbrCount, rbrNanos / 1_000_000.0);
            System.out.printf("Batched   : %d rows in %.1f ms%n", batchCount, batchNanos / 1_000_000.0);

            if (rbrCount == N && batchCount == N) {
                System.out.println("PASSED: both strategies inserted all " + N + " rows.");
                passed++;
            } else { System.out.println("FAILED: insert counts mismatch."); failed++; }

            // Sanity: no rows lost or duplicated between the two tables.
            if (rbrCount == batchCount) {
                System.out.println("PASSED: batched insert produced the same row count as row-by-row.");
                passed++;
            } else { System.out.println("FAILED: batch vs row-by-row counts differ."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: batch operations verified."
                : "FAILED: batch operations have errors.");
    }
}
