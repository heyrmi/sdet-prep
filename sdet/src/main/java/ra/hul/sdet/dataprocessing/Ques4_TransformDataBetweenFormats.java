package ra.hul.sdet.dataprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform Data Between Formats - Convert CSV to JSON and back, preserving integrity.
 * Common SDET question: "Read CSV, produce JSON, convert JSON back to CSV and prove the
 * round-trip (A -> B -> A) is lossless, handling quoted fields with commas/quotes."
 *
 * Self-contained: input CSV is an inline string; JSON handled by a small hand-rolled
 * serializer/parser (NO external libs, NO network).
 */
public class Ques4_TransformDataBetweenFormats {

    // ---------- CSV parsing (RFC-4180-ish: quotes, escaped quotes, embedded commas) ----------
    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else {
                switch (c) {
                    case '"' -> inQuotes = true;
                    case ',' -> { out.add(cur.toString()); cur.setLength(0); }
                    default -> cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    static String toCsvField(String v) {
        boolean needsQuote = v.contains(",") || v.contains("\"") || v.contains("\n");
        if (!needsQuote) return v;
        return '"' + v.replace("\"", "\"\"") + '"';
    }

    /** CSV text -> list of ordered field maps (keyed by header). */
    static List<Map<String, String>> csvToRecords(String csv) {
        String[] lines = csv.strip().split("\n");
        List<String> headers = parseCsvLine(lines[0]);
        List<Map<String, String>> records = new ArrayList<>();
        for (int r = 1; r < lines.length; r++) {
            List<String> fields = parseCsvLine(lines[r]);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                row.put(headers.get(c), c < fields.size() ? fields.get(c) : "");
            }
            records.add(row);
        }
        return records;
    }

    /** Ordered field maps -> CSV text (header row derived from first record's keys). */
    static String recordsToCsv(List<Map<String, String>> records) {
        if (records.isEmpty()) return "";
        List<String> headers = new ArrayList<>(records.get(0).keySet());
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers.stream().map(Ques4_TransformDataBetweenFormats::toCsvField).toList()));
        sb.append('\n');
        for (Map<String, String> row : records) {
            List<String> cells = new ArrayList<>();
            for (String h : headers) cells.add(toCsvField(row.getOrDefault(h, "")));
            sb.append(String.join(",", cells)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    // ---------- JSON serialize (array of string-valued objects) ----------
    static String recordsToJson(List<Map<String, String>> records) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, String> e : records.get(i).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                jsonString(e.getKey(), sb);
                sb.append(':');
                jsonString(e.getValue(), sb);
            }
            sb.append('}');
        }
        return sb.append(']').toString();
    }

    static void jsonString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        sb.append('"');
    }

    // ---------- JSON parse (only array-of-flat-objects with string values needed here) ----------
    static List<Map<String, String>> jsonToRecords(String json) {
        MiniJson p = new MiniJson(json);
        return p.parseArrayOfObjects();
    }

    static final class MiniJson {
        private final String s;
        private int i;

        MiniJson(String s) { this.s = s; }

        List<Map<String, String>> parseArrayOfObjects() {
            List<Map<String, String>> out = new ArrayList<>();
            ws();
            expect('[');
            ws();
            if (peek() == ']') { i++; return out; }
            while (true) {
                out.add(object());
                ws();
                char c = s.charAt(i++);
                if (c == ']') break;
                if (c != ',') throw new IllegalArgumentException("Expected , or ] at " + (i - 1));
                ws();
            }
            return out;
        }

        private Map<String, String> object() {
            Map<String, String> m = new LinkedHashMap<>();
            ws();
            expect('{');
            ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws();
                String key = string();
                ws();
                expect(':');
                ws();
                String val = string();
                m.put(key, val);
                ws();
                char c = s.charAt(i++);
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("Expected , or } at " + (i - 1));
            }
            return m;
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> throw new IllegalArgumentException("Bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char peek() { return s.charAt(i); }
        private void expect(char c) {
            if (s.charAt(i) != c) throw new IllegalArgumentException("Expected '" + c + "' at " + i);
            i++;
        }
    }

    static void main() {
        // CSV with a quoted field containing a comma and embedded quotes — the tricky case.
        // (Plain string literal, not a text block, because the data contains a "" "" sequence
        //  that would collide with text-block triple-quote delimiters.)
        String csv =
                "id,name,note\n" +
                "1,Widget,\"cheap, small\"\n" +
                "2,\"Gadget \"\"Pro\"\"\",flagship\n" +
                "3,Gizmo,plain\n";

        System.out.println("--- Original CSV ---");
        System.out.println(csv.strip());

        // CSV -> records -> JSON
        List<Map<String, String>> records = csvToRecords(csv);
        String json = recordsToJson(records);
        System.out.println("\n--- CSV -> JSON ---");
        System.out.println(json);

        // JSON -> records -> CSV
        List<Map<String, String>> back = jsonToRecords(json);
        String csvBack = recordsToCsv(back);
        System.out.println("\n--- JSON -> CSV ---");
        System.out.println(csvBack);

        // Round-trip integrity checks
        check("record count preserved", records.size() == 3 && back.size() == 3);
        check("embedded comma preserved", "cheap, small".equals(back.get(0).get("note")));
        check("escaped quotes preserved", "Gadget \"Pro\"".equals(back.get(1).get("name")));
        check("data model equal after A->B->A", records.equals(back));
        check("re-serialized CSV equals canonical CSV", recordsToCsv(records).equals(csvBack));

        System.out.println("\nPASSED: CSV <-> JSON round-trip preserves data including quoted/escaped fields.");
    }

    static void check(String label, boolean ok) {
        System.out.println((ok ? "  PASS: " : "  FAIL: ") + label);
        if (!ok) throw new AssertionError("Check failed: " + label);
    }
}
