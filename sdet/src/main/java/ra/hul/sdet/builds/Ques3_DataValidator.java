package ra.hul.sdet.builds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Data Validator - Validate records against configurable rules and produce an error report.
 * Common SDET question (machine-coding round): "Read records; validate each against rules
 * (required, type, range/length, regex, cross-field); collect all errors per record;
 * support custom rules via interface/lambda."
 *
 * Self-contained: builds sample records (as Maps) in main(), validates, prints a report.
 */
public class Ques3_DataValidator {

    /** A single validation failure. */
    public record ValidationError(int recordIndex, String field, String rule, String message) {}

    /** A rule that inspects a whole record and reports 0..n errors. Custom rules plug in as lambdas. */
    @FunctionalInterface
    public interface Rule {
        void validate(int index, Map<String, Object> record, List<ValidationError> errors);
    }

    // ---- Built-in rule factories -------------------------------------------------

    public static Rule required(String field) {
        return (i, rec, errs) -> {
            Object v = rec.get(field);
            if (v == null || v.toString().isBlank())
                errs.add(new ValidationError(i, field, "required", field + " is required"));
        };
    }

    /** Type check: "number", "email". Skips when value absent (use required() to enforce presence). */
    public static Rule type(String field, String type) {
        return (i, rec, errs) -> {
            Object v = rec.get(field);
            if (v == null) return;
            String s = v.toString();
            boolean ok = switch (type) {
                case "number" -> s.matches("-?\\d+(\\.\\d+)?");
                case "email" -> Pattern.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$", s);
                default -> true;
            };
            if (!ok) errs.add(new ValidationError(i, field, "type:" + type, field + " is not a valid " + type));
        };
    }

    /** Numeric range check (inclusive). */
    public static Rule range(String field, double min, double max) {
        return (i, rec, errs) -> {
            Object v = rec.get(field);
            if (v == null) return;
            try {
                double d = Double.parseDouble(v.toString());
                if (d < min || d > max)
                    errs.add(new ValidationError(i, field, "range",
                            field + "=" + d + " out of [" + min + "," + max + "]"));
            } catch (NumberFormatException e) {
                errs.add(new ValidationError(i, field, "range", field + " is not numeric"));
            }
        };
    }

    /** String length check (inclusive). */
    public static Rule length(String field, int min, int max) {
        return (i, rec, errs) -> {
            Object v = rec.get(field);
            if (v == null) return;
            int len = v.toString().length();
            if (len < min || len > max)
                errs.add(new ValidationError(i, field, "length",
                        field + " length " + len + " out of [" + min + "," + max + "]"));
        };
    }

    public static Rule regex(String field, String pattern) {
        Pattern p = Pattern.compile(pattern);
        return (i, rec, errs) -> {
            Object v = rec.get(field);
            if (v == null) return;
            if (!p.matcher(v.toString()).matches())
                errs.add(new ValidationError(i, field, "regex", field + " does not match " + pattern));
        };
    }

    /** Cross-field rule: end must be strictly greater than start (numeric or ISO date lexical compare). */
    public static Rule greaterThan(String bigger, String smaller) {
        return (i, rec, errs) -> {
            Object a = rec.get(bigger), b = rec.get(smaller);
            if (a == null || b == null) return;
            if (a.toString().compareTo(b.toString()) <= 0)
                errs.add(new ValidationError(i, bigger, "cross-field",
                        bigger + " (" + a + ") must be > " + smaller + " (" + b + ")"));
        };
    }

    /** Run all rules over all records, collecting every error. */
    public static List<ValidationError> validate(List<Map<String, Object>> records, List<Rule> rules) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < records.size(); i++)
            for (Rule r : rules) r.validate(i, records.get(i), errors);
        return errors;
    }

    private static Map<String, Object> rec(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static void main() {
        List<Map<String, Object>> records = List.of(
                rec("name", "Alice", "email", "alice@example.com", "age", "30",
                        "start", "2026-01-01", "end", "2026-12-31"),
                rec("name", "", "email", "not-an-email", "age", "200",
                        "start", "2026-06-01", "end", "2026-01-01"),   // required+length+email+range+cross-field
                rec("name", "Bob", "email", "bob@corp.io", "age", "abc",
                        "start", "2026-01-01", "end", "2026-02-01")     // age fails both type and range parse
        );

        List<Rule> rules = List.of(
                required("name"),
                type("email", "email"),
                type("age", "number"),
                range("age", 0, 120),
                length("name", 1, 50),
                greaterThan("end", "start"),
                // custom rule via lambda: name must be capitalized
                (i, r, errs) -> {
                    Object n = r.get("name");
                    if (n != null && !n.toString().isEmpty() && !Character.isUpperCase(n.toString().charAt(0)))
                        errs.add(new ValidationError(i, "name", "custom:capitalized", "name must start uppercase"));
                });

        List<ValidationError> errors = validate(records, rules);

        System.out.println("=== Validation Report ===");
        for (ValidationError e : errors)
            System.out.printf("record #%d | field=%s | rule=%s | %s%n",
                    e.recordIndex(), e.field(), e.rule(), e.message());
        System.out.println("Total errors: " + errors.size());

        long rec0 = errors.stream().filter(e -> e.recordIndex() == 0).count();
        long rec1 = errors.stream().filter(e -> e.recordIndex() == 1).count();
        long rec2 = errors.stream().filter(e -> e.recordIndex() == 2).count();
        // rec0 clean; rec1: required+length+email+range+cross-field = 5; rec2: age type + range-parse = 2
        boolean ok = rec0 == 0 && rec1 == 5 && rec2 == 2;
        System.out.println(ok ? "PASSED: per-record error counts match expected (0/5/2)."
                : "FAILED: got " + rec0 + "/" + rec1 + "/" + rec2 + ".");
    }
}
