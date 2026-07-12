package ra.hul.sdet.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DB Testing & Schema Validation - how an SDET validates a database with DatabaseMetaData.
 * Teaches: introspecting a schema (columns, types, nullability, primary keys) via DatabaseMetaData
 * and asserting it matches an expected contract; verifying a foreign key is enforced (an orphan child
 * insert throws); and row-count reconciliation between related tables. Interview angle: "how do you
 * test a database as an SDET?" — assert the schema contract, referential integrity, and reconciliation
 * counts, not just that queries return something.
 *
 * Self-contained: builds a parent/child schema in H2 and asserts schema, FK enforcement, and counts.
 */
public class Ques7_DbTestingAndSchemaValidation {

    private static final String URL = "jdbc:h2:mem:ques7_schema;DB_CLOSE_DELAY=-1";

    /** Expected column contract for the ORDERS table: column name -> (java.sql.Types, nullable). */
    record ColumnSpec(int sqlType, boolean nullable) {}

    static void createAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(100) NOT NULL)");
            st.execute("""
                    CREATE TABLE orders (
                        id          INT PRIMARY KEY,
                        customer_id INT NOT NULL,
                        amount      DECIMAL(10,2) NOT NULL,
                        note        VARCHAR(200),
                        CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
                    )
                    """);
            st.execute("INSERT INTO customers VALUES (1, 'Alice'), (2, 'Bob')");
            st.execute("INSERT INTO orders VALUES (100, 1, 50.00, 'first'), (101, 1, 25.00, NULL), (102, 2, 75.00, 'third')");
        }
    }

    static void main() throws SQLException {
        int passed = 0, failed = 0;
        try (Connection c = DriverManager.getConnection(URL)) {
            createAndSeed(c);
            DatabaseMetaData meta = c.getMetaData();

            // 1) Column contract: name -> (type, nullable). H2 stores unquoted identifiers upper-case.
            Map<String, ColumnSpec> expected = new LinkedHashMap<>();
            expected.put("ID", new ColumnSpec(java.sql.Types.INTEGER, false));
            expected.put("CUSTOMER_ID", new ColumnSpec(java.sql.Types.INTEGER, false));
            expected.put("AMOUNT", new ColumnSpec(java.sql.Types.DECIMAL, false));
            expected.put("NOTE", new ColumnSpec(java.sql.Types.VARCHAR, true));

            Map<String, ColumnSpec> actual = new LinkedHashMap<>();
            try (ResultSet rs = meta.getColumns(null, null, "ORDERS", null)) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    int type = rs.getInt("DATA_TYPE");
                    boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    actual.put(col, new ColumnSpec(type, nullable));
                }
            }
            System.out.println("ORDERS columns: " + actual.keySet());
            if (actual.equals(expected)) {
                System.out.println("PASSED: ORDERS schema matches expected column/type/nullability contract.");
                passed++;
            } else { System.out.println("FAILED: schema contract mismatch. actual=" + actual); failed++; }

            // 2) Primary key introspection.
            String pkColumn = null;
            try (ResultSet rs = meta.getPrimaryKeys(null, null, "ORDERS")) {
                if (rs.next()) pkColumn = rs.getString("COLUMN_NAME");
            }
            System.out.println("ORDERS primary key: " + pkColumn);
            if ("ID".equals(pkColumn)) { System.out.println("PASSED: primary key is ID."); passed++; }
            else { System.out.println("FAILED: expected primary key ID."); failed++; }

            // 3) Foreign-key enforcement: inserting an orphan order (no such customer) must throw.
            boolean fkEnforced = false;
            try (Statement st = c.createStatement()) {
                st.execute("INSERT INTO orders VALUES (200, 999, 10.00, 'orphan')");
            } catch (SQLException expectedViolation) {
                fkEnforced = true;
                System.out.println("Orphan insert rejected: " + expectedViolation.getMessage().split("\n")[0]);
            }
            if (fkEnforced) { System.out.println("PASSED: referential integrity enforced (orphan child rejected)."); passed++; }
            else { System.out.println("FAILED: FK constraint did not fire."); failed++; }

            // 4) Row-count reconciliation: every order must reference an existing customer.
            long orders, orphans;
            try (Statement st = c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders")) { rs.next(); orders = rs.getLong(1); }
                try (ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE c.id IS NULL")) {
                    rs.next(); orphans = rs.getLong(1);
                }
            }
            System.out.println("orders=" + orders + " orphans=" + orphans);
            if (orders == 3 && orphans == 0) {
                System.out.println("PASSED: reconciliation clean — 3 orders, 0 orphaned rows.");
                passed++;
            } else { System.out.println("FAILED: reconciliation found orphans or wrong count."); failed++; }
        }
        System.out.println("=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0 ? "PASSED: DB schema validation verified."
                : "FAILED: DB schema validation has errors.");
    }
}
