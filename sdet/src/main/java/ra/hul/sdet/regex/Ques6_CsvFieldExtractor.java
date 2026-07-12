package ra.hul.sdet.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CSV Field Extractor - Split a CSV record honouring quoted fields, embedded commas/newlines and "" escapes.
 * Common SDET question: "Parse a CSV row where a field may contain commas, newlines and doubled quotes".
 *
 * NOTE (interview point): regex handles a single record well, but for streaming huge files or ragged
 * quoting a state-machine / library (OpenCSV, Commons CSV) is safer. Self-contained: main() self-verifies.
 */
public class Ques6_CsvFieldExtractor {

    // Each field is either "quoted (with "" escapes)" or an unquoted run, anchored on start-or-comma.
    private static final Pattern FIELD =
            Pattern.compile("(?:^|,)(?:\"((?:[^\"]|\"\")*)\"|([^\",]*))");

    public static List<String> parse(String record) {
        List<String> fields = new ArrayList<>();
        Matcher m = FIELD.matcher(record);
        while (m.find()) {
            if (m.group(1) != null) {
                fields.add(m.group(1).replace("\"\"", "\"")); // unescape doubled quotes
            } else {
                fields.add(m.group(2));
            }
        }
        return fields;
    }

    static void main() {
        record Case(String csv, List<String> expected) {}
        Case[] cases = {
                new Case("John,Doe,30", List.of("John", "Doe", "30")),
                new Case("John,\"Doe, Jr.\",\"New York\"", List.of("John", "Doe, Jr.", "New York")),
                new Case("id,\"Say \"\"Hi\"\" now\",end", List.of("id", "Say \"Hi\" now", "end")),
                new Case("a,,c", List.of("a", "", "c")),
                new Case("\"line1\nline2\",next", List.of("line1\nline2", "next"))
        };

        boolean pass = true;
        for (Case c : cases) {
            List<String> got = parse(c.csv());
            boolean ok = got.equals(c.expected());
            System.out.printf("%-30s -> %s%n", c.csv().replace("\n", "\\n"), got);
            pass &= ok;
        }
        System.out.println(pass ? "PASS: quoted fields, escaped quotes, commas and newlines parsed correctly."
                : "FAIL: CSV parsing mismatch.");
    }
}
