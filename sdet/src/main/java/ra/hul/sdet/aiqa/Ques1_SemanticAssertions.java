package ra.hul.sdet.aiqa;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Semantic Assertions - how to assert on output that is never byte-identical twice.
 *
 * <p>The first thing that breaks when you test an LLM feature is {@code assertEquals}. The same
 * prompt returns different wording every call, so exact matching fails on correct answers. The
 * answer is not "give up on assertions" — it is a LADDER of assertion strengths, and you pick the
 * strongest rung that the output can actually satisfy:
 *
 * <pre>
 *   STRONGEST  exact match          - classification labels, extracted IDs, enum outputs
 *              structural           - valid JSON, required fields present, types correct
 *              contains / excludes   - key facts present, forbidden claims absent
 *              fuzzy (edit distance) - near-identical with formatting drift
 *   WEAKEST    semantic similarity   - free-form prose, meaning-preserving paraphrase
 * </pre>
 *
 * <p>The mistake candidates make is jumping straight to the weakest rung ("just use an embedding
 * similarity") for outputs that are perfectly capable of exact assertion. A classifier that must
 * emit one of five labels should be asserted exactly — anything looser hides real regressions.
 *
 * <p>Interview angle: "how do you test something non-deterministic?" — present the ladder, then
 * note that most of an AI system's surface (structure, extraction, classification) sits on the
 * strong rungs, and only free-form prose needs the weak ones.
 *
 * <p>Self-contained: no network, no model calls. Embeddings are supplied as fixed vectors.
 */
public class Ques1_SemanticAssertions {

    /** Outcome of one assertion, with the reason so a failure is diagnosable. */
    record AssertionResult(boolean passed, String rung, String detail) {
        static AssertionResult pass(String rung) {
            return new AssertionResult(true, rung, "ok");
        }

        static AssertionResult fail(String rung, String detail) {
            return new AssertionResult(false, rung, detail);
        }
    }

    // ---------- Rung 1: exact ----------

    /** Use for classification labels, extracted identifiers, enum outputs. */
    static AssertionResult assertExact(String actual, String expected) {
        return actual.equals(expected)
                ? AssertionResult.pass("exact")
                : AssertionResult.fail("exact", "expected \"" + expected + "\" but got \"" + actual + "\"");
    }

    // ---------- Rung 2: structural ----------

    /**
     * Minimal check that a response is a JSON object carrying every required key. A real suite
     * would use a schema validator; the point here is that structure is fully deterministic and
     * therefore assertable at full strength.
     */
    static AssertionResult assertStructure(String actual, List<String> requiredKeys) {
        String trimmed = actual.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return AssertionResult.fail("structural", "not a JSON object: " + preview(trimmed));
        }
        List<String> missing = new ArrayList<>();
        for (String key : requiredKeys) {
            if (!trimmed.contains("\"" + key + "\"")) {
                missing.add(key);
            }
        }
        return missing.isEmpty()
                ? AssertionResult.pass("structural")
                : AssertionResult.fail("structural", "missing required keys: " + missing);
    }

    // ---------- Rung 3: contains / excludes ----------

    /**
     * The workhorse for graded answers: the facts that MUST appear, and the claims that must NOT.
     *
     * <p>{@code mustNotContain} is the half people forget, and it is the half that catches the
     * dangerous failures — an answer that is broadly right but adds "guaranteed" or "always" to a
     * policy statement is a legal problem, not a wording quibble.
     */
    static AssertionResult assertContains(String actual, List<String> mustContain, List<String> mustNotContain) {
        String lower = actual.toLowerCase(Locale.ROOT);

        List<String> missing = new ArrayList<>();
        for (String phrase : mustContain) {
            if (!lower.contains(phrase.toLowerCase(Locale.ROOT))) {
                missing.add(phrase);
            }
        }
        List<String> forbidden = new ArrayList<>();
        for (String phrase : mustNotContain) {
            if (lower.contains(phrase.toLowerCase(Locale.ROOT))) {
                forbidden.add(phrase);
            }
        }

        if (missing.isEmpty() && forbidden.isEmpty()) {
            return AssertionResult.pass("contains");
        }
        return AssertionResult.fail("contains",
                (missing.isEmpty() ? "" : "missing " + missing + " ")
                        + (forbidden.isEmpty() ? "" : "forbidden " + forbidden));
    }

    // ---------- Rung 4: fuzzy ----------

    /** Levenshtein distance — the classic edit-distance DP, O(m*n) time, O(n) space. */
    static int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[b.length()];
    }

    /** Similarity in [0,1]: 1 - normalised edit distance. Two empty strings are identical. */
    static double fuzzySimilarity(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) editDistance(a, b) / maxLen;
    }

    static AssertionResult assertFuzzy(String actual, String expected, double threshold) {
        double sim = fuzzySimilarity(actual, expected);
        return sim >= threshold
                ? AssertionResult.pass("fuzzy")
                : AssertionResult.fail("fuzzy",
                        String.format("similarity %.3f below threshold %.3f", sim, threshold));
    }

    // ---------- Rung 5: semantic ----------

    /** Cosine similarity; returns 0 rather than NaN for degenerate input so sorts stay sane. */
    static double cosineSimilarity(double[] a, double[] b) {
        if (a.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0, magA = 0, magB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            magA += a[i] * a[i];
            magB += b[i] * b[i];
        }
        if (magA == 0 || magB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    /**
     * The weakest rung: two texts mean roughly the same thing. Use only for free-form prose, and
     * be aware embeddings are poor at exactly the distinctions that flip an answer — negation,
     * small numeric differences, and named entities. "Refunds are allowed" and "refunds are not
     * allowed" embed very close together.
     */
    static AssertionResult assertSemantic(double[] actualEmbedding, double[] expectedEmbedding, double threshold) {
        double sim = cosineSimilarity(actualEmbedding, expectedEmbedding);
        return sim >= threshold
                ? AssertionResult.pass("semantic")
                : AssertionResult.fail("semantic",
                        String.format("cosine %.3f below threshold %.3f", sim, threshold));
    }

    private static String preview(String s) {
        return s.length() <= 40 ? s : s.substring(0, 40) + "...";
    }

    static void main() {
        int passed = 0, failed = 0;

        System.out.println("=== The assertion ladder ===\n");

        // --- Rung 1: exact, for a classification label ---
        System.out.println("[exact] classification output");
        AssertionResult r = assertExact("BILLING", "BILLING");
        System.out.println("  same label            -> " + r.passed());
        if (r.passed()) passed++; else failed++;

        r = assertExact("billing", "BILLING");
        System.out.println("  case drift            -> " + r.passed() + " (" + r.detail() + ")");
        if (!r.passed()) passed++; else failed++;   // must fail: a label enum is exact or it is broken

        // --- Rung 2: structural ---
        System.out.println("\n[structural] tool-call / JSON output");
        r = assertStructure("{\"intent\":\"refund\",\"confidence\":0.91}", List.of("intent", "confidence"));
        System.out.println("  all keys present      -> " + r.passed());
        if (r.passed()) passed++; else failed++;

        r = assertStructure("{\"intent\":\"refund\"}", List.of("intent", "confidence"));
        System.out.println("  missing a key         -> " + r.passed() + " (" + r.detail() + ")");
        if (!r.passed()) passed++; else failed++;

        r = assertStructure("Sure! Here is the JSON: {\"intent\":\"refund\"}", List.of("intent"));
        System.out.println("  chatty preamble       -> " + r.passed() + " (" + r.detail() + ")");
        if (!r.passed()) passed++; else failed++;   // a very common real failure

        // --- Rung 3: contains / excludes ---
        System.out.println("\n[contains] graded policy answer");
        String good = "Refunds are issued within 30 days for standard plans; enterprise customers "
                + "may request an exception.";
        r = assertContains(good, List.of("30 days", "enterprise"), List.of("guaranteed", "always"));
        System.out.println("  correct answer        -> " + r.passed());
        if (r.passed()) passed++; else failed++;

        String overclaim = "Refunds are always guaranteed within 30 days for enterprise customers.";
        r = assertContains(overclaim, List.of("30 days", "enterprise"), List.of("guaranteed", "always"));
        System.out.println("  overclaiming answer   -> " + r.passed() + " (" + r.detail() + ")");
        if (!r.passed()) passed++; else failed++;   // contains-only would have PASSED this

        // --- Rung 4: fuzzy ---
        System.out.println("\n[fuzzy] formatting drift");
        r = assertFuzzy("Order #4021 shipped on 2026-03-01",
                "Order #4021 shipped on 2026-03-01.", 0.95);
        System.out.println("  trailing period       -> " + r.passed()
                + String.format(" (sim %.3f)", fuzzySimilarity(
                        "Order #4021 shipped on 2026-03-01", "Order #4021 shipped on 2026-03-01.")));
        if (r.passed()) passed++; else failed++;

        r = assertFuzzy("Order #4021 was cancelled", "Order #4021 shipped on 2026-03-01", 0.95);
        System.out.println("  different fact        -> " + r.passed());
        if (!r.passed()) passed++; else failed++;

        // --- Rung 5: semantic, and its blind spot ---
        System.out.println("\n[semantic] paraphrase vs negation");
        double[] refundAllowed = {0.90, 0.30, 0.10};
        double[] paraphrase    = {0.88, 0.34, 0.12};   // "you may get a refund"
        double[] negation      = {0.86, 0.33, 0.16};   // "you may NOT get a refund"

        r = assertSemantic(paraphrase, refundAllowed, 0.95);
        System.out.println("  true paraphrase       -> " + r.passed()
                + String.format(" (cos %.4f)", cosineSimilarity(paraphrase, refundAllowed)));
        if (r.passed()) passed++; else failed++;

        double negSim = cosineSimilarity(negation, refundAllowed);
        r = assertSemantic(negation, refundAllowed, 0.95);
        System.out.println("  NEGATED meaning       -> " + r.passed()
                + String.format(" (cos %.4f)  <-- the blind spot", negSim));
        System.out.println("      embeddings barely distinguish negation, so semantic similarity");
        System.out.println("      ALONE would accept the opposite answer. Pair it with a");
        System.out.println("      mustNotContain check on negation words for anything load-bearing.");
        if (r.passed()) passed++; else failed++;   // it does pass — that is the lesson

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: assertion ladder behaves as documented at every rung."
                : "FAIL: assertion ladder mismatch.");
    }
}
