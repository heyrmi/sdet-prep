package ra.hul.sdet.regex;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * URL Parser - Split a URL into components and parse its query string into a map.
 * Common SDET question: "Extract protocol, host, port, path, query params and fragment from a URL".
 *
 * Self-contained: main() self-verifies with PASS/FAIL. Uses java.net.URI plus manual query decoding.
 */
public class Ques3_UrlParser {

    /** Parsed URL components. Port is -1 when not present in the URL. */
    public record ParsedUrl(String protocol, String host, int port, String path,
                            Map<String, String> query, String fragment) {}

    public static ParsedUrl parse(String url) {
        URI uri = URI.create(url);
        Map<String, String> query = parseQuery(uri.getRawQuery());
        return new ParsedUrl(uri.getScheme(), uri.getHost(), uri.getPort(),
                uri.getPath(), query, uri.getFragment());
    }

    /** Parses a raw (still-encoded) query string into an insertion-ordered decoded map. */
    public static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                map.put(decode(pair), "");
            } else {
                map.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return map;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static void main() {
        String url = "https://user.example.com:8443/api/v1/search?q=hello%20world&lang=en&page=2#results";
        ParsedUrl p = parse(url);

        System.out.println("protocol = " + p.protocol());
        System.out.println("host     = " + p.host());
        System.out.println("port     = " + p.port());
        System.out.println("path     = " + p.path());
        System.out.println("query    = " + p.query());
        System.out.println("fragment = " + p.fragment());

        boolean pass =
                p.protocol().equals("https")
                        && p.host().equals("user.example.com")
                        && p.port() == 8443
                        && p.path().equals("/api/v1/search")
                        && p.query().get("q").equals("hello world")   // %20 decoded
                        && p.query().get("lang").equals("en")
                        && p.query().get("page").equals("2")
                        && p.fragment().equals("results");

        // Edge case: no port, no query, no fragment.
        ParsedUrl simple = parse("http://localhost/health");
        pass &= simple.port() == -1 && simple.query().isEmpty() && simple.fragment() == null;

        System.out.println(pass ? "PASS: URL components and query map parsed correctly."
                : "FAIL: URL parsing mismatch.");
    }
}
