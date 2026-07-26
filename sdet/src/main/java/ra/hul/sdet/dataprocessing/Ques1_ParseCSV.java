package ra.hul.sdet.dataprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parse CSV - Read a CSV file, parse it, and compute summary statistics.
 * Common SDET question: "Parse this CSV of employees and find avg salary per department."
 *
 * Usage: Run main() — reads employees.csv from src/main/resources/testdata/
 */
public class Ques1_ParseCSV {

    record Employee(String name, String department, double salary) {}

    /**
     * Resolves a test-data file through the classpath rather than a repo-relative path, so the
     * problem runs the same from the IDE, from `mvn exec:java`, and from the CI verifier — all of
     * which have different working directories.
     */
    static String testDataPath(String fileName) throws IOException {
        java.net.URL url = Ques1_ParseCSV.class.getResource("/testdata/" + fileName);
        if (url == null) {
            throw new IllegalStateException("testdata/" + fileName + " is not on the classpath");
        }
        if ("file".equals(url.getProtocol())) {
            try {
                return java.nio.file.Path.of(url.toURI()).toString();
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        // Packaged in a jar — there is no real file to point FileReader at, so stage a temp copy.
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("testdata-", "-" + fileName);
        temp.toFile().deleteOnExit();
        try (java.io.InputStream in = Ques1_ParseCSV.class.getResourceAsStream("/testdata/" + fileName)) {
            java.nio.file.Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return temp.toString();
    }

    public static List<Employee> parseCSV(String filePath) throws IOException {
        List<Employee> employees = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                employees.add(new Employee(parts[0].trim(), parts[1].trim(), Double.parseDouble(parts[2].trim())));
            }
        }
        return employees;
    }

    static void main() throws IOException {
        List<Employee> employees = parseCSV(testDataPath("employees.csv"));

        System.out.println("All Employees:");
        employees.forEach(e -> System.out.printf("  %-12s %-12s $%.2f%n", e.name(), e.department(), e.salary()));

        System.out.println("\nAverage Salary by Department:");
        Map<String, Double> avgByDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::department, Collectors.averagingDouble(Employee::salary)));
        avgByDept.forEach((dept, avg) -> System.out.printf("  %-12s $%.2f%n", dept, avg));

        System.out.println("\nHighest Paid Employee:");
        employees.stream()
                .max((a, b) -> Double.compare(a.salary(), b.salary()))
                .ifPresent(e -> System.out.printf("  %s (%s) - $%.2f%n", e.name(), e.department(), e.salary()));

        System.out.println();
        System.out.println(!employees.isEmpty() && avgByDept.values().stream().allMatch(v -> v > 0)
                ? "PASSED: CSV parsed into " + employees.size() + " employees across "
                        + avgByDept.size() + " departments."
                : "FAIL: CSV parsing mismatch.");
    }
}
