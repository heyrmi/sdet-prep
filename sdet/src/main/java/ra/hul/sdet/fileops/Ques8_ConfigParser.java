package ra.hul.sdet.fileops;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Config Parser - Parse an .ini/.properties style file: sections, key=value, comments, ${var} interpolation.
 * Common SDET question: "Parse a config file with [sections], comments, and variable substitution."
 *
 * Self-contained: builds a sample config temp file in main(), parses it, prints values, cleans up.
 * Interpolation resolves ${key} (current section first, then the [default]/global section).
 */
public class Ques8_ConfigParser {

    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]+)}");
    public static final String DEFAULT_SECTION = "default";

    /** Parsed config: section name -> (key -> raw value). Order preserved. */
    public static Map<String, Map<String, String>> parse(Path file) throws IOException {
        Map<String, Map<String, String>> config = new LinkedHashMap<>();
        config.put(DEFAULT_SECTION, new LinkedHashMap<>());
        String current = DEFAULT_SECTION;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                    continue; // blank or comment
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    current = trimmed.substring(1, trimmed.length() - 1).strip();
                    config.computeIfAbsent(current, k -> new LinkedHashMap<>());
                    continue;
                }
                int eq = indexOfSeparator(trimmed);
                if (eq < 0) continue; // malformed, skip gracefully
                String key = trimmed.substring(0, eq).strip();
                String value = trimmed.substring(eq + 1).strip();
                config.get(current).put(key, value);
            }
        }
        return config;
    }

    private static int indexOfSeparator(String s) {
        int eq = s.indexOf('=');
        int colon = s.indexOf(':');
        if (eq < 0) return colon;
        if (colon < 0) return eq;
        return Math.min(eq, colon);
    }

    /** Resolve ${var} references in a value, checking the given section then [default]. */
    public static String resolve(Map<String, Map<String, String>> config, String section, String value) {
        Matcher m = VAR.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1).strip();
            String replacement = lookup(config, section, name);
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    replacement != null ? replacement : m.group(0)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String lookup(Map<String, Map<String, String>> config, String section, String name) {
        Map<String, String> sec = config.get(section);
        if (sec != null && sec.containsKey(name)) return resolve(config, section, sec.get(name));
        Map<String, String> def = config.get(DEFAULT_SECTION);
        if (def != null && def.containsKey(name)) return resolve(config, DEFAULT_SECTION, def.get(name));
        return null;
    }

    static void main() throws IOException {
        Path cfg = Files.createTempFile("app", ".ini");
        try {
            String content = """
                    # global defaults
                    root = /opt/app
                    env = prod

                    [database]
                    ; connection settings
                    host = db.local
                    port = 5432
                    url = jdbc:postgresql://${host}:${port}/mydb

                    [paths]
                    logs = ${root}/logs
                    """;
            Files.writeString(cfg, content);

            Map<String, Map<String, String>> config = parse(cfg);

            String dbUrl = resolve(config, "database", config.get("database").get("url"));
            String logsPath = resolve(config, "paths", config.get("paths").get("logs"));

            System.out.println("=== Config Parser ===");
            System.out.println("Sections     : " + config.keySet());
            System.out.println("database.url : " + dbUrl);
            System.out.println("paths.logs   : " + logsPath);
            System.out.println("default.env  : " + config.get(DEFAULT_SECTION).get("env"));

            boolean ok = dbUrl.equals("jdbc:postgresql://db.local:5432/mydb")
                    && logsPath.equals("/opt/app/logs")
                    && config.containsKey("database")
                    && config.get(DEFAULT_SECTION).get("env").equals("prod");
            System.out.println(ok ? "PASSED: sections parsed and ${var} interpolation resolved."
                    : "FAILED: parse/interpolation mismatch.");
        } finally {
            Files.deleteIfExists(cfg);
        }
    }
}
