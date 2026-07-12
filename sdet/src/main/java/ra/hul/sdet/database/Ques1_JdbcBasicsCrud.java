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
 * JDBC Basics + CRUD - connect, DDL, and the full INSERT/SELECT/UPDATE/DELETE lifecycle.
 * Teaches: opening a Connection, executing DDL, PreparedStatement CRUD, and mapping a ResultSet
 * row into an immutable Java record. Interview angle: "walk me through reading a row with JDBC" —
 * the answer is Connection -> PreparedStatement -> ResultSet -> map columns by name into a POJO/record.
 *
 * Self-contained: spins up an in-memory H2 database, creates schema, exercises CRUD, and asserts results.
 */
public class Ques1_JdbcBasicsCrud {

    private static final String URL = "jdbc:h2:mem:ques1_crud;DB_CLOSE_DELAY=-1";

    /** Immutable view of one row of the USERS table. */
    public record User(long id, String name, String email, int age) {}

    static void createSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE users (
                        id    BIGINT PRIMARY KEY,
                        name  VARCHAR(100) NOT NULL,
                        email VARCHAR(200) NOT NULL,
                        age   INT NOT NULL
                    )
                    """);
        }
    }

    static int insertUser(Connection c, User u) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (id, name, email, age) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, u.id());
            ps.setString(2, u.name());
            ps.setString(3, u.email());
            ps.setInt(4, u.age());
            return ps.executeUpdate();
        }
    }

    static User findById(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, name, email, age FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        }
    }

    static List<User> findAll(Connection c) throws SQLException {
        List<User> out = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, email, age FROM users ORDER BY id")) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    static int updateAge(Connection c, long id, int newAge) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET age = ? WHERE id = ?")) {
            ps.setInt(1, newAge);
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }

    static int deleteById(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    static long count(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Map the current ResultSet row into a User record. */
    static User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("name"),
                rs.getString("email"), rs.getInt("age"));
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createSchema(c);

            // CREATE
            insertUser(c, new User(1, "Alice", "alice@example.com", 30));
            insertUser(c, new User(2, "Bob", "bob@example.com", 25));
            insertUser(c, new User(3, "Carol", "carol@example.com", 41));
            System.out.println("Seeded rows: " + count(c));

            // READ (single, mapped into a record)
            User bob = findById(c, 2);
            System.out.println("findById(2) -> " + bob);
            if (bob != null && bob.name().equals("Bob") && bob.age() == 25) {
                System.out.println("PASSED: SELECT mapped ResultSet row into record correctly.");
                passed++;
            } else { System.out.println("FAILED: mapped row mismatch."); failed++; }

            // READ (all)
            List<User> all = findAll(c);
            if (all.size() == 3 && all.get(0).name().equals("Alice")) {
                System.out.println("PASSED: findAll returned 3 ordered rows.");
                passed++;
            } else { System.out.println("FAILED: findAll count/order mismatch."); failed++; }

            // UPDATE
            int updated = updateAge(c, 1, 31);
            User alice = findById(c, 1);
            if (updated == 1 && alice.age() == 31) {
                System.out.println("PASSED: UPDATE changed exactly one row (age 30 -> 31).");
                passed++;
            } else { System.out.println("FAILED: UPDATE did not persist."); failed++; }

            // DELETE
            int deleted = deleteById(c, 3);
            if (deleted == 1 && count(c) == 2 && findById(c, 3) == null) {
                System.out.println("PASSED: DELETE removed the row; count back to 2.");
                passed++;
            } else { System.out.println("FAILED: DELETE did not remove row."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: JDBC CRUD lifecycle verified."
                : "FAILED: JDBC CRUD lifecycle has errors.");
    }
}
