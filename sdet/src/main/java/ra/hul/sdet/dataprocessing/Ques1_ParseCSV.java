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
 * Usage: Run main() — reads employees.csv from sdet/src/main/resources/testdata/
 */
public class Ques1_ParseCSV {

    record Employee(String name, String department, double salary) {}

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

    public static void main(String[] args) throws IOException {
        String filePath = "sdet/src/main/resources/testdata/employees.csv";
        List<Employee> employees = parseCSV(filePath);

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
    }
}
