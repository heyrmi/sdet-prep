package ra.hul.sdet.propertytesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Metamorphic Testing - testing what you cannot compute the right answer for.
 *
 * <p>Sometimes there is no oracle. What is the *correct* ranking for a search query? The correct
 * summary of a document? The correct route between two cities? If you could compute the right
 * answer cheaply you would not need the system.
 *
 * <p>Metamorphic testing sidesteps the missing oracle entirely. You cannot say what the output
 * should BE, but you can say how it must CHANGE when the input changes in a known way. Those
 * relations are <b>metamorphic relations</b>, and violating one is a bug even though you never
 * knew the right answer.
 *
 * <pre>
 *   search("cheap laptop")   vs  search("laptop cheap")   -> results should be near-identical
 *   route(A -> B)            vs  route(B -> A)            -> same distance
 *   classify(text)           vs  classify(text + " ")     -> same label
 *   sum(list)                vs  sum(shuffle(list))       -> same total
 * </pre>
 *
 * <p>This is the technique that transfers directly to AI systems, where the missing oracle is the
 * whole problem — see {@link ra.hul.sdet.aiqa.Ques1_SemanticAssertions}. It is also the honest
 * answer to "how do you test a recommendation engine / search ranker / ML model?", which is a
 * common interview question with very few good answers.
 *
 * <p>Self-contained: a small search ranker with a seeded generator, plus a deliberately broken
 * variant to prove the relations catch real defects.
 */
public class Ques3_MetamorphicTesting {

    record Document(String id, String text) {
    }

    static final List<Document> CORPUS = List.of(
            new Document("d1", "cheap laptop with long battery life"),
            new Document("d2", "premium laptop for designers"),
            new Document("d3", "cheap phone accessories and cases"),
            new Document("d4", "laptop repair services near you"),
            new Document("d5", "budget desktop computer deals"),
            new Document("d6", "wireless keyboard and mouse bundle"));

    static List<String> tokens(String s) {
        List<String> out = new ArrayList<>();
        for (String t : s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /** A correct ranker: score by how many query terms a document contains, ties by document ID. */
    static List<String> rank(String query, List<Document> corpus) {
        List<String> terms = tokens(query);
        record Scored(String id, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Document d : corpus) {
            List<String> docTerms = tokens(d.text());
            int score = 0;
            for (String t : terms) {
                if (docTerms.contains(t)) {
                    score++;
                }
            }
            if (score > 0) {
                scored.add(new Scored(d.id(), score));
            }
        }
        scored.sort((a, b) -> a.score() != b.score()
                ? Integer.compare(b.score(), a.score())
                : a.id().compareTo(b.id()));
        return scored.stream().map(Scored::id).toList();
    }

    /**
     * A broken ranker: it weights terms by their POSITION in the query, so reordering the words
     * changes the results. No example-based test would notice unless someone happened to write
     * the reordered case.
     */
    static List<String> rankBuggy(String query, List<Document> corpus) {
        List<String> terms = tokens(query);
        record Scored(String id, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Document d : corpus) {
            List<String> docTerms = tokens(d.text());
            int score = 0;
            for (int i = 0; i < terms.size(); i++) {
                if (docTerms.contains(terms.get(i))) {
                    score += (terms.size() - i) * 10;   // position-weighted — the defect
                }
            }
            if (score > 0) {
                scored.add(new Scored(d.id(), score));
            }
        }
        scored.sort((a, b) -> a.score() != b.score()
                ? Integer.compare(b.score(), a.score())
                : a.id().compareTo(b.id()));
        return scored.stream().map(Scored::id).toList();
    }

    interface Ranker {
        List<String> rank(String query, List<Document> corpus);
    }

    // ---------- metamorphic relations ----------

    /**
     * MR1: permuting the query terms must not change the result set or its order.
     *
     * <p>Uses REVERSAL rather than a shuffle. A seeded shuffle can return the original order,
     * and then the relation passes without ever testing anything — a vacuous pass, which is the
     * most dangerous kind of green test. Reversal is deterministic and always a real permutation
     * for two or more terms.
     */
    static boolean permutationInvariant(Ranker r, String query) {
        List<String> terms = new ArrayList<>(tokens(query));
        if (terms.size() < 2) {
            return true;   // nothing to permute
        }
        List<String> reversed = new ArrayList<>(terms);
        java.util.Collections.reverse(reversed);
        return r.rank(String.join(" ", terms), CORPUS)
                .equals(r.rank(String.join(" ", reversed), CORPUS));
    }

    /** MR2: extra whitespace and case changes must not affect ranking. */
    static boolean formattingInvariant(Ranker r, String query) {
        String noisy = "  " + query.toUpperCase(Locale.ROOT).replace(" ", "   ") + "  ";
        return r.rank(query, CORPUS).equals(r.rank(noisy, CORPUS));
    }

    /**
     * MR3: adding an irrelevant document to the corpus must not reorder the existing results.
     * A ranker whose scores depend on corpus-wide state can violate this, and it is one of the
     * hardest ranking bugs to find any other way.
     */
    static boolean insertionStable(Ranker r, String query) {
        List<String> before = r.rank(query, CORPUS);
        List<Document> extended = new ArrayList<>(CORPUS);
        extended.add(new Document("zzz", "completely unrelated gardening equipment"));
        List<String> after = new ArrayList<>(r.rank(query, extended));
        after.remove("zzz");
        return before.equals(after);
    }

    /** MR4: narrowing the query (adding a term) cannot ADD documents to the result set. */
    static boolean narrowingReducesResults(Ranker r, String query) {
        List<String> broad = r.rank(query, CORPUS);
        List<String> narrow = r.rank(query + " xyzzy", CORPUS);
        return broad.containsAll(narrow);
    }

    record RelationReport(String name, boolean heldForCorrect, boolean heldForBuggy) {
    }

    static void main() {
        int passed = 0, failed = 0;

        Ranker correct = Ques3_MetamorphicTesting::rank;
        Ranker buggy = Ques3_MetamorphicTesting::rankBuggy;

        List<String> queries = List.of(
                "cheap laptop", "laptop battery life", "cheap phone", "budget computer deals");

        System.out.println("=== Metamorphic relations on a search ranker ===");
        System.out.println("(we never state the CORRECT ranking — only how it must behave)\n");

        List<RelationReport> reports = new ArrayList<>();

        boolean permCorrect = true, permBuggy = true;
        for (String q : queries) {
            permCorrect &= permutationInvariant(correct, q);
            permBuggy &= permutationInvariant(buggy, q);
        }
        reports.add(new RelationReport("MR1 permutation-invariant", permCorrect, permBuggy));

        boolean fmtCorrect = true, fmtBuggy = true;
        for (String q : queries) {
            fmtCorrect &= formattingInvariant(correct, q);
            fmtBuggy &= formattingInvariant(buggy, q);
        }
        reports.add(new RelationReport("MR2 formatting-invariant", fmtCorrect, fmtBuggy));

        boolean insCorrect = true, insBuggy = true;
        for (String q : queries) {
            insCorrect &= insertionStable(correct, q);
            insBuggy &= insertionStable(buggy, q);
        }
        reports.add(new RelationReport("MR3 insertion-stable", insCorrect, insBuggy));

        boolean narrowCorrect = true, narrowBuggy = true;
        for (String q : queries) {
            narrowCorrect &= narrowingReducesResults(correct, q);
            narrowBuggy &= narrowingReducesResults(buggy, q);
        }
        reports.add(new RelationReport("MR4 narrowing-reduces", narrowCorrect, narrowBuggy));

        System.out.printf("%-28s %10s %10s%n", "relation", "correct", "buggy");
        for (RelationReport r : reports) {
            System.out.printf("%-28s %10s %10s%n", r.name(), r.heldForCorrect(), r.heldForBuggy());
        }

        System.out.println("\n--- the demonstration ---");
        String q = "cheap laptop";
        System.out.printf("  buggy.rank(\"cheap laptop\")  -> %s%n", rankBuggy(q, CORPUS));
        System.out.printf("  buggy.rank(\"laptop cheap\")  -> %s%n", rankBuggy("laptop cheap", CORPUS));
        System.out.println("  ^ same query, reordered words, different ranking. No oracle was");
        System.out.println("    needed to know that is wrong.");

        System.out.println("\n--- checks ---");

        boolean c1 = reports.stream().allMatch(RelationReport::heldForCorrect);
        System.out.println("every relation holds for the correct ranker : " + c1);
        if (c1) passed++; else failed++;

        boolean c2 = permCorrect && !permBuggy;
        System.out.println("MR1 catches the position-weighting bug      : " + c2);
        if (c2) passed++; else failed++;

        boolean c3 = reports.stream().anyMatch(r -> r.heldForCorrect() && !r.heldForBuggy());
        System.out.println("at least one relation separates the two     : " + c3);
        if (c3) passed++; else failed++;

        // The relations are cheap and composable: none required knowing a right answer.
        boolean c4 = insCorrect && narrowCorrect;
        System.out.println("stability and narrowing hold for correct    : " + c4);
        if (c4) passed++; else failed++;

        // Determinism, so a failure is reproducible.
        boolean c5 = rank(q, CORPUS).equals(rank(q, CORPUS));
        System.out.println("ranking is deterministic                    : " + c5);
        if (c5) passed++; else failed++;

        System.out.println("\n--- where this transfers ---");
        System.out.println("  search / ranking : reorder terms, add irrelevant docs, paginate");
        System.out.println("  ML classifiers   : semantically-neutral rewrites keep the label");
        System.out.println("  LLM features     : paraphrase the prompt, expect an equivalent answer");
        System.out.println("  compilers        : -O0 and -O2 must produce the same behaviour");
        System.out.println("  numeric code     : scale all inputs, expect a predictable scaling");

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: metamorphic relations found a bug with no oracle available."
                : "FAIL: metamorphic testing mismatch.");
    }
}
