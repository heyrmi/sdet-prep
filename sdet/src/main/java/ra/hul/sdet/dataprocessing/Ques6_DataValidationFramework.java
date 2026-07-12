package ra.hul.sdet.dataprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Data Validation Framework - A reusable, builder-configured validator that checks records
 * against rules (required, type, numeric range, regex, custom) and collects ALL errors per record.
 * Common SDET question: "Build a small validation framework: define rules per field, run every
 * record through them, and report every violation (not just the first)."
 *
 * Self-contained: records are inline Map<String,String> rows (NO files, NO network).
 */
public class Ques6_DataValidationFramework {

    /** One violation: which record index, which field, what went wrong. */
    record Violation(int recordIndex, String field, String message) {
        @Override public String toString() { return "record[" + recordIndex + "]." + field + ": " + message; }
    }

    /** A single field rule. name is for error messages; test returns true when the value is INVALID. */
    record Rule(String field, String description, Predicate<String> isInvalid) {}

    /** Fluent builder to assemble rules for a schema. */
    static final class Validator {
        private final List<Rule> rules = new ArrayList<>();

        Validator required(String field) {
            rules.add(new Rule(field, "required",
                    v -> v == null || v.isBlank()));
            return this;
        }

        Validator isInteger(String field) {
            rules.add(new Rule(field, "must be an integer",
                    v -> v != null && !v.isBlank() && !v.matches("-?\\d+")));
            return this;
        }

        Validator isDouble(String field) {
            rules.add(new Rule(field, "must be a number", v -> {
                if (v == null || v.isBlank()) return false; // presence handled by required()
                try { Double.parseDouble(v); return false; } catch (NumberFormatException e) { return true; }
            }));
            return this;
        }

        Validator range(String field, double min, double max) {
            rules.add(new Rule(field, "must be in [" + min + ", " + max + "]", v -> {
                if (v == null || v.isBlank()) return false;
                try { double d = Double.parseDouble(v); return d < min || d > max; }
                catch (NumberFormatException e) { return false; } // type error reported elsewhere
            }));
            return this;
        }

        Validator matches(String field, String regex, String desc) {
            Pattern p = Pattern.compile(regex);
            rules.add(new Rule(field, desc, v -> v != null && !v.isBlank() && !p.matcher(v).matches()));
            return this;
        }

        /** Custom rule escape hatch. */
        Validator custom(String field, String desc, Predicate<String> isInvalid) {
            rules.add(new Rule(field, desc, isInvalid));
            return this;
        }

        /** Validate one record; returns all violations for it. */
        List<Violation> validate(int index, Map<String, String> record) {
            List<Violation> violations = new ArrayList<>();
            for (Rule rule : rules) {
                if (rule.isInvalid().test(record.get(rule.field()))) {
                    violations.add(new Violation(index, rule.field(),
                            rule.description() + " (was: " + repr(record.get(rule.field())) + ")"));
                }
            }
            return violations;
        }

        /** Validate a batch; returns all violations across all records. */
        List<Violation> validateAll(List<Map<String, String>> records) {
            List<Violation> all = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) all.addAll(validate(i, records.get(i)));
            return all;
        }

        private static String repr(String v) { return v == null ? "<missing>" : "'" + v + "'"; }
    }

    static Map<String, String> row(String id, String age, String email, String score) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("age", age);
        m.put("email", email);
        m.put("score", score);
        return m;
    }

    static void main() {
        Validator validator = new Validator()
                .required("id")
                .isInteger("id")
                .required("age").isInteger("age").range("age", 0, 120)
                .required("email").matches("email", "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "must be a valid email")
                .required("score").isDouble("score").range("score", 0, 100);

        List<Map<String, String>> records = List.of(
                row("1", "30", "ada@example.com", "88.5"),      // valid
                row("", "200", "not-an-email", "abc"),          // 5 problems: id required, age range, email, score type... (+score range skipped since type failed)
                row("3", "twenty", "bob@x.io", "150"),          // age not int, score out of range
                row("4", "45", "carol@corp.co", "72")           // valid
        );

        List<Violation> violations = validator.validateAll(records);

        System.out.println("Validation report (" + violations.size() + " violations):");
        violations.forEach(v -> System.out.println("  " + v));

        // Group per record for stats
        Map<Integer, List<Violation>> byRecord = new LinkedHashMap<>();
        for (Violation v : violations) byRecord.computeIfAbsent(v.recordIndex(), k -> new ArrayList<>()).add(v);

        System.out.println("\nPer-record summary:");
        for (int i = 0; i < records.size(); i++) {
            int count = byRecord.getOrDefault(i, List.of()).size();
            System.out.printf("  record[%d]: %s%n", i, count == 0 ? "VALID" : count + " error(s)");
        }

        // Expected: record 0 & 3 valid; record 1 has id(required)+age(range)+email+score(type) = 4;
        // record 2 has age(type)+score(range) = 2.
        check("record 0 valid", byRecord.getOrDefault(0, List.of()).isEmpty());
        check("record 1 has 4 errors", byRecord.getOrDefault(1, List.of()).size() == 4);
        check("record 2 has 2 errors", byRecord.getOrDefault(2, List.of()).size() == 2);
        check("record 3 valid", byRecord.getOrDefault(3, List.of()).isEmpty());
        check("all errors collected (not fail-fast)", violations.size() == 6);
        check("valid record passes cleanly",
                validator.validate(0, row("9", "20", "z@z.zz", "10")).isEmpty());

        System.out.println("\nPASSED: rule-based validator collects every violation per record.");
    }

    static void check(String label, boolean ok) {
        System.out.println((ok ? "  PASS: " : "  FAIL: ") + label);
        if (!ok) throw new AssertionError("Check failed: " + label);
    }
}
