package ra.hul.sdet.dataprocessing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON to Java Object Mapping - Parse a complex nested JSON response into Java POJOs.
 * Common SDET question: "Deserialize a nested JSON (arrays, nested objects, optional/null
 * fields, dates) into POJOs, then serialize back and prove the round-trip is lossless."
 *
 * Self-contained: uses a small hand-rolled JSON parser/serializer (NO external libs, NO network).
 * Note: rest-assured declares jackson-databind as an *optional* dependency, so it is NOT on the
 * compile classpath transitively — hence the hand-rolled mapper here keeps this dependency-safe.
 */
public class Ques2_JsonToObjectMapping {

    // ----- POJOs (nested object graph with optional fields) -----
    record Address(String city, String zip) {}

    record User(long id, String name, String email /* optional, may be null */,
                Address address, List<String> roles, LocalDate createdAt) {}

    // ----- Mapping: JSON tree (Map/List/...) -> POJO -----
    @SuppressWarnings("unchecked")
    static User userFromJson(String json) {
        Map<String, Object> m = (Map<String, Object>) JsonParser.parse(json);
        Map<String, Object> addr = (Map<String, Object>) m.get("address");
        List<String> roles = new ArrayList<>();
        Object rawRoles = m.get("roles");
        if (rawRoles instanceof List<?> l) {
            for (Object r : l) roles.add((String) r);
        }
        return new User(
                ((Number) m.get("id")).longValue(),
                (String) m.get("name"),
                (String) m.get("email"),                       // missing/null tolerated
                addr == null ? null : new Address((String) addr.get("city"), (String) addr.get("zip")),
                roles,
                m.get("createdAt") == null ? null : LocalDate.parse((String) m.get("createdAt")));
    }

    // ----- Mapping: POJO -> JSON tree -> JSON text -----
    static String userToJson(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id());
        m.put("name", u.name());
        m.put("email", u.email());
        if (u.address() != null) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("city", u.address().city());
            a.put("zip", u.address().zip());
            m.put("address", a);
        } else {
            m.put("address", null);
        }
        m.put("roles", u.roles());
        m.put("createdAt", u.createdAt() == null ? null : u.createdAt().toString());
        return JsonSerializer.toJson(m);
    }

    static void main() {
        String json = """
                {
                  "id": 42,
                  "name": "Ada Lovelace",
                  "email": null,
                  "address": { "city": "London", "zip": "EC1A" },
                  "roles": ["ADMIN", "ENGINEER"],
                  "createdAt": "2026-07-12"
                }
                """;

        // 1) Deserialize nested JSON -> POJO (handles null email + nested object + array + date)
        User u = userFromJson(json);
        System.out.println("Deserialized: " + u);
        check("id parsed", u.id() == 42);
        check("nested city parsed", "London".equals(u.address().city()));
        check("array roles parsed", u.roles().equals(List.of("ADMIN", "ENGINEER")));
        check("null email tolerated", u.email() == null);
        check("date parsed", LocalDate.of(2026, 7, 12).equals(u.createdAt()));

        // 2) Serialize POJO -> JSON, then deserialize again -> POJO (round-trip)
        String out = userToJson(u);
        System.out.println("Serialized:   " + out);
        User back = userFromJson(out);
        check("round-trip lossless", Objects.equals(u, back));

        // 3) Missing optional field ("email" absent entirely) must map to null, not crash
        User missing = userFromJson("{\"id\":7,\"name\":\"Bob\",\"roles\":[]}");
        check("missing fields tolerated", missing.email() == null && missing.address() == null
                && missing.roles().isEmpty());

        System.out.println("PASSED: JSON <-> POJO mapping with nesting, arrays, nulls, dates and round-trip.");
    }

    static void check(String label, boolean ok) {
        System.out.println((ok ? "  PASS: " : "  FAIL: ") + label);
        if (!ok) throw new AssertionError("Check failed: " + label);
    }

    // ===================== Hand-rolled JSON parser =====================
    static final class JsonParser {
        private final String s;
        private int i;

        private JsonParser(String s) { this.s = s; }

        static Object parse(String s) {
            JsonParser p = new JsonParser(s);
            p.ws();
            Object v = p.value();
            p.ws();
            if (p.i != s.length()) throw new IllegalArgumentException("Trailing content at " + p.i);
            return v;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        private Object value() {
            ws();
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't', 'f' -> bool();
                case 'n' -> nul();
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // consume {
            ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                String key = string();
                ws();
                expect(':');
                m.put(key, value());
                ws();
                char c = s.charAt(i++);
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("Expected , or } at " + (i - 1));
            }
            return m;
        }

        private List<Object> array() {
            List<Object> a = new ArrayList<>();
            i++; // consume [
            ws();
            if (s.charAt(i) == ']') { i++; return a; }
            while (true) {
                a.add(value());
                ws();
                char c = s.charAt(i++);
                if (c == ']') break;
                if (c != ',') throw new IllegalArgumentException("Expected , or ] at " + (i - 1));
            }
            return a;
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
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                        default -> throw new IllegalArgumentException("Bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String num = s.substring(start, i);
            if (num.contains(".") || num.contains("e") || num.contains("E")) return Double.parseDouble(num);
            return Long.parseLong(num);
        }

        private Boolean bool() {
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            throw new IllegalArgumentException("Bad literal at " + i);
        }

        private Object nul() {
            if (s.startsWith("null", i)) { i += 4; return null; }
            throw new IllegalArgumentException("Bad literal at " + i);
        }

        private void expect(char c) {
            if (s.charAt(i) != c) throw new IllegalArgumentException("Expected '" + c + "' at " + i);
            i++;
        }
    }

    // ===================== Hand-rolled JSON serializer =====================
    static final class JsonSerializer {
        static String toJson(Object o) {
            StringBuilder sb = new StringBuilder();
            write(o, sb);
            return sb.toString();
        }

        private static void write(Object o, StringBuilder sb) {
            switch (o) {
                case null -> sb.append("null");
                case String str -> writeString(str, sb);
                case Boolean b -> sb.append(b);
                case Number n -> sb.append(n);
                case Map<?, ?> m -> {
                    sb.append('{');
                    boolean first = true;
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        if (!first) sb.append(',');
                        first = false;
                        writeString(String.valueOf(e.getKey()), sb);
                        sb.append(':');
                        write(e.getValue(), sb);
                    }
                    sb.append('}');
                }
                case List<?> l -> {
                    sb.append('[');
                    for (int k = 0; k < l.size(); k++) {
                        if (k > 0) sb.append(',');
                        write(l.get(k), sb);
                    }
                    sb.append(']');
                }
                default -> writeString(o.toString(), sb);
            }
        }

        private static void writeString(String str, StringBuilder sb) {
            sb.append('"');
            for (int k = 0; k < str.length(); k++) {
                char c = str.charAt(k);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            sb.append('"');
        }
    }
}
