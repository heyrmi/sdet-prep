package ra.hul.sdet.regex;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * HTML Tag Stripper - Remove HTML markup, leaving readable text content.
 * Common SDET question: "Strip tags including script/style blocks and decode common entities".
 *
 * NOTE (interview point): regex is fine for simple sanitising but NOT a substitute for a real HTML
 * parser (Jsoup) — malformed/nested markup and CDATA can defeat it. Self-contained: main() self-verifies.
 */
public class Ques5_HtmlTagStripper {

    // Remove <script>...</script> and <style>...</style> including their inner content first.
    private static final Pattern SCRIPT_STYLE =
            Pattern.compile("(?is)<(script|style)\\b[^>]*>.*?</\\1>");
    // Any remaining tag (open, close, self-closing, with attributes).
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");
    // Collapse runs of whitespace produced by removed block-level tags.
    private static final Pattern WS = Pattern.compile("\\s+");

    private static final Map<String, String> ENTITIES = Map.of(
            "&amp;", "&", "&lt;", "<", "&gt;", ">", "&quot;", "\"",
            "&#39;", "'", "&apos;", "'", "&nbsp;", " ");

    public static String strip(String html) {
        if (html == null) return "";
        String s = SCRIPT_STYLE.matcher(html).replaceAll(" ");
        s = TAG.matcher(s).replaceAll(" ");
        for (Map.Entry<String, String> e : ENTITIES.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }
        return WS.matcher(s).replaceAll(" ").trim();
    }

    static void main() {
        record Case(String html, String expected) {}
        Case[] cases = {
                new Case("<p>Hello <b>World</b></p>", "Hello World"),
                new Case("<div class=\"x\">Nested <span>text <i>here</i></span></div>", "Nested text here"),
                new Case("Line<br/>break and <img src=\"a.png\"/> image", "Line break and image"),
                new Case("<style>.a{color:red}</style><p>Only text</p><script>alert(1)</script>", "Only text"),
                new Case("A &amp; B &lt;tag&gt; &quot;quoted&quot;", "A & B <tag> \"quoted\"")
        };

        boolean pass = true;
        for (Case c : cases) {
            String got = strip(c.html());
            boolean ok = got.equals(c.expected());
            System.out.printf("%-55s -> \"%s\"%n", c.html(), got);
            pass &= ok;
        }
        System.out.println(pass ? "PASS: tags stripped, script/style removed, entities decoded."
                : "FAIL: HTML stripping mismatch.");
    }
}
