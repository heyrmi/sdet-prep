package ra.hul.sdet.builds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration Manager - Merge config from multiple sources with a priority order and typed getters.
 * Common SDET question (machine-coding round): "Build a config manager merging defaults < file < env <
 * CLI args, supporting nested keys (database.host) and type-safe getString/getInt/getBoolean/getList."
 *
 * Self-contained: builds a temp .properties file and a simulated env map / CLI args in main(),
 * merges them, verifies resolution order, cleans up.
 */
public class Ques5_ConfigurationManager {

    // Flattened key -> value. Later put() wins, so load sources lowest-priority first.
    private final Map<String, String> values = new LinkedHashMap<>();

    /** Layer 1 (lowest): programmatic defaults. */
    public Ques5_ConfigurationManager withDefaults(Map<String, String> defaults) {
        defaults.forEach(values::put);
        return this;
    }

    /** Layer 2: a .properties file (supports dotted nested keys directly). */
    public Ques5_ConfigurationManager withPropertiesFile(Path file) throws IOException {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) { p.load(in); }
        p.forEach((k, v) -> values.put(k.toString(), v.toString()));
        return this;
    }

    /** Layer 3: environment variables. DATABASE_HOST -> database.host. */
    public Ques5_ConfigurationManager withEnv(Map<String, String> env, String prefix) {
        env.forEach((k, v) -> {
            if (prefix == null || k.startsWith(prefix)) {
                String key = (prefix == null ? k : k.substring(prefix.length()))
                        .toLowerCase().replace('_', '.');
                values.put(key, v);
            }
        });
        return this;
    }

    /** Layer 4 (highest): CLI args of the form --database.host=localhost or --flag. */
    public Ques5_ConfigurationManager withArgs(String[] args) {
        for (String a : args) {
            if (!a.startsWith("--")) continue;
            String body = a.substring(2);
            int eq = body.indexOf('=');
            if (eq >= 0) values.put(body.substring(0, eq), body.substring(eq + 1));
            else values.put(body, "true");
        }
        return this;
    }

    // ---- Typed getters -----------------------------------------------------------

    public String getString(String key) { return values.get(key); }

    public String getString(String key, String def) { return values.getOrDefault(key, def); }

    public int getInt(String key) { return Integer.parseInt(require(key)); }

    public boolean getBoolean(String key) { return Boolean.parseBoolean(require(key)); }

    /** Comma-separated list getter. */
    public List<String> getList(String key) {
        String v = values.get(key);
        if (v == null || v.isBlank()) return List.of();
        return Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** All keys under a nested prefix, e.g. sub("database") -> {host=..., port=...}. */
    public Map<String, String> sub(String prefix) {
        String p = prefix.endsWith(".") ? prefix : prefix + ".";
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach((k, v) -> { if (k.startsWith(p)) out.put(k.substring(p.length()), v); });
        return out;
    }

    private String require(String key) {
        String v = values.get(key);
        if (v == null) throw new IllegalStateException("Missing config key: " + key);
        return v;
    }

    static void main() throws IOException {
        Path propsFile = Files.createTempFile("app", ".properties");
        try {
            Files.writeString(propsFile, """
                    database.host=file-host
                    database.port=5432
                    feature.flags=a,b,c
                    app.debug=false
                    """);

            // env overrides file for database.host; args override everything for database.port
            Map<String, String> env = Map.of("APP_DATABASE_HOST", "env-host");
            String[] cli = {"--database.port=6543", "--app.debug=true", "--verbose"};

            var cfg = new Ques5_ConfigurationManager()
                    .withDefaults(Map.of("database.host", "default-host", "timeout", "30"))
                    .withPropertiesFile(propsFile)
                    .withEnv(env, "APP_")
                    .withArgs(cli);

            System.out.println("=== Configuration Manager (defaults < file < env < CLI) ===");
            System.out.println("database.host = " + cfg.getString("database.host") + "  (env wins)");
            System.out.println("database.port = " + cfg.getInt("database.port") + "  (CLI wins)");
            System.out.println("app.debug     = " + cfg.getBoolean("app.debug") + "  (CLI wins)");
            System.out.println("timeout       = " + cfg.getInt("timeout") + "  (default)");
            System.out.println("feature.flags = " + cfg.getList("feature.flags"));
            System.out.println("verbose       = " + cfg.getBoolean("verbose") + "  (flag)");
            System.out.println("sub(database) = " + cfg.sub("database"));

            boolean ok = cfg.getString("database.host").equals("env-host")     // env > file > default
                    && cfg.getInt("database.port") == 6543                      // CLI > file
                    && cfg.getBoolean("app.debug")                              // CLI > file(false)
                    && cfg.getInt("timeout") == 30                              // default survives
                    && cfg.getList("feature.flags").equals(List.of("a", "b", "c"))
                    && cfg.getBoolean("verbose")                                // bare --flag
                    && cfg.sub("database").size() == 2;
            System.out.println(ok ? "PASSED: priority resolution, typed getters, and nested keys correct."
                    : "FAILED: config resolution mismatch.");
        } finally {
            Files.deleteIfExists(propsFile);
        }
    }
}
