package ra.hul.sdet.aiqa;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Retrieval Quality Metrics - the deterministic half of testing a RAG system.
 *
 * <p>Most reported "hallucinations" are not generation failures at all: the right chunk was never
 * retrieved, and the model faithfully summarised the wrong one. That makes retrieval the first
 * thing to measure, and the good news is it is measurable with classical IR metrics — no model
 * call, no judge, no flakiness.
 *
 * <p>The four that matter:
 *
 * <ul>
 *   <li><b>recall@k</b> — of the truly relevant documents, how many made the top k? <i>The</i> RAG
 *       metric. If the answer was never retrieved, no prompt engineering can recover it.</li>
 *   <li><b>precision@k</b> — of the k returned, how many were relevant? Low precision wastes
 *       context budget and measurably degrades answers.</li>
 *   <li><b>MRR</b> — 1/rank of the first relevant hit. Rewards putting the right thing first,
 *       which matters because models attend unevenly across a long context.</li>
 *   <li><b>NDCG@k</b> — rank-weighted with GRADED relevance, for when "relevant" is not binary.</li>
 * </ul>
 *
 * <p>Interview angle: "how do you test a RAG system?" — measure retrieval separately from
 * generation. Separating those two layers is what makes a failure attributable, and it is the
 * single highest-signal thing you can say on this topic.
 *
 * <p>Self-contained: fixed ranked lists and labelled relevance judgements.
 */
public class Ques3_RetrievalQualityMetrics {

    /** One evaluation query: what was returned, and what should have been. */
    record RetrievalCase(
            String query,
            List<String> retrieved,      // ranked, best first
            Set<String> relevant,        // ground truth, binary
            Map<String, Integer> graded  // ground truth, graded 0-3 (for NDCG)
    ) {
    }

    // ---------- recall@k ----------

    /**
     * Share of relevant documents that appear in the top k.
     *
     * <p>Returns 1.0 when there are no relevant documents — vacuously, nothing was missed.
     * Returning 0 there would drag down every average with cases that carry no signal.
     */
    static double recallAtK(List<String> retrieved, Set<String> relevant, int k) {
        if (relevant.isEmpty()) {
            return 1.0;
        }
        int found = 0;
        for (String doc : retrieved.subList(0, Math.min(k, retrieved.size()))) {
            if (relevant.contains(doc)) {
                found++;
            }
        }
        return (double) found / relevant.size();
    }

    // ---------- precision@k ----------

    /**
     * Share of the top k that are relevant.
     *
     * <p>Note the denominator is k, not the number retrieved. A system that returns 3 documents
     * when asked for 10 has not earned full precision — it under-delivered.
     */
    static double precisionAtK(List<String> retrieved, Set<String> relevant, int k) {
        if (k <= 0) {
            return 0;
        }
        int found = 0;
        for (String doc : retrieved.subList(0, Math.min(k, retrieved.size()))) {
            if (relevant.contains(doc)) {
                found++;
            }
        }
        return (double) found / k;
    }

    // ---------- MRR ----------

    /** Reciprocal rank of the first relevant document; 0 if none was retrieved. */
    static double reciprocalRank(List<String> retrieved, Set<String> relevant) {
        for (int i = 0; i < retrieved.size(); i++) {
            if (relevant.contains(retrieved.get(i))) {
                return 1.0 / (i + 1);   // ranks are 1-based
            }
        }
        return 0;
    }

    /** Mean reciprocal rank across a set of cases. */
    static double meanReciprocalRank(List<RetrievalCase> cases) {
        if (cases.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (RetrievalCase c : cases) {
            sum += reciprocalRank(c.retrieved(), c.relevant());
        }
        return sum / cases.size();
    }

    // ---------- NDCG@k ----------

    /**
     * Discounted Cumulative Gain: sum of gain/log2(rank+1) over the top k.
     * The logarithm is what encodes "position 1 matters much more than position 8".
     */
    static double dcgAtK(List<String> retrieved, Map<String, Integer> graded, int k) {
        double dcg = 0;
        int limit = Math.min(k, retrieved.size());
        for (int i = 0; i < limit; i++) {
            int gain = graded.getOrDefault(retrieved.get(i), 0);
            dcg += gain / (Math.log(i + 2) / Math.log(2));   // log2(i+2) == log2(rank+1)
        }
        return dcg;
    }

    /**
     * NDCG@k — DCG normalised by the DCG of the ideal ordering, so scores are comparable across
     * queries with different numbers of relevant documents.
     */
    static double ndcgAtK(List<String> retrieved, Map<String, Integer> graded, int k) {
        double dcg = dcgAtK(retrieved, graded, k);

        // Ideal ordering: every graded document, best relevance first.
        List<String> ideal = graded.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        double idcg = dcgAtK(ideal, graded, k);

        return idcg == 0 ? 0 : dcg / idcg;
    }

    // ---------- demo data ----------

    static List<RetrievalCase> cases() {
        return List.of(
                // Ideal: both relevant chunks retrieved, ranked first.
                new RetrievalCase("how long for a refund",
                        List.of("refund-policy", "refund-enterprise", "shipping", "holidays", "errors"),
                        Set.of("refund-policy", "refund-enterprise"),
                        Map.of("refund-policy", 3, "refund-enterprise", 2, "shipping", 0)),

                // Recall is fine, but the relevant doc is buried at rank 4. Models attend least
                // to the middle of a long context, so rank matters beyond mere presence.
                new RetrievalCase("payment declined error",
                        List.of("shipping", "holidays", "refund-policy", "errors", "refund-enterprise"),
                        Set.of("errors"),
                        Map.of("errors", 3, "refund-policy", 1)),

                // The failure that produces a "hallucination" complaint: the answer was never
                // retrieved at all. No amount of prompt tuning fixes this case.
                new RetrievalCase("office closure dates",
                        List.of("refund-policy", "shipping", "errors", "refund-enterprise"),
                        Set.of("holidays"),
                        Map.of("holidays", 3)));
    }

    static void main() {
        int passed = 0, failed = 0;
        List<RetrievalCase> cases = cases();

        System.out.println("=== Retrieval metrics per query (k=5) ===\n");
        System.out.printf("%-26s %8s %8s %8s %8s%n", "query", "R@5", "P@5", "RR", "NDCG@5");
        for (RetrievalCase c : cases) {
            System.out.printf("%-26s %8.2f %8.2f %8.2f %8.3f%n",
                    c.query(),
                    recallAtK(c.retrieved(), c.relevant(), 5),
                    precisionAtK(c.retrieved(), c.relevant(), 5),
                    reciprocalRank(c.retrieved(), c.relevant()),
                    ndcgAtK(c.retrieved(), c.graded(), 5));
        }

        System.out.println("\n--- checks ---");
        RetrievalCase ideal = cases.get(0);
        RetrievalCase buried = cases.get(1);
        RetrievalCase missed = cases.get(2);

        // 1. Perfect retrieval scores 1.0 recall.
        boolean c1 = recallAtK(ideal.retrieved(), ideal.relevant(), 5) == 1.0;
        System.out.println("both relevant docs retrieved -> recall 1.0        : " + c1);
        if (c1) passed++; else failed++;

        // 2. recall@k is sensitive to k — the whole point of reporting the k.
        double r1 = recallAtK(ideal.retrieved(), ideal.relevant(), 1);
        boolean c2 = r1 == 0.5;
        System.out.printf("recall@1 = %.2f (only 1 of 2 relevant in top 1)   : %s%n", r1, c2);
        if (c2) passed++; else failed++;

        // 3. The critical case: nothing relevant retrieved. Recall 0 and RR 0.
        boolean c3 = recallAtK(missed.retrieved(), missed.relevant(), 5) == 0.0
                && reciprocalRank(missed.retrieved(), missed.relevant()) == 0.0;
        System.out.println("relevant doc never retrieved -> recall 0, RR 0    : " + c3);
        System.out.println("    ^ this is what most 'the model hallucinated' reports actually are");
        if (c3) passed++; else failed++;

        // 4. MRR distinguishes rank-1 from rank-4 even when recall is identical.
        double rrIdeal = reciprocalRank(ideal.retrieved(), ideal.relevant());
        double rrBuried = reciprocalRank(buried.retrieved(), buried.relevant());
        boolean c4 = rrIdeal == 1.0 && Math.abs(rrBuried - 0.25) < 1e-9 && rrIdeal > rrBuried;
        System.out.printf("RR rank1=%.2f > RR rank4=%.2f (recall same)       : %s%n", rrIdeal, rrBuried, c4);
        if (c4) passed++; else failed++;

        // 5. Precision penalises padding the top-k with irrelevant chunks.
        double p5 = precisionAtK(ideal.retrieved(), ideal.relevant(), 5);
        boolean c5 = Math.abs(p5 - 0.4) < 1e-9;
        System.out.printf("precision@5 = %.2f (2 relevant of 5 returned)     : %s%n", p5, c5);
        System.out.println("    ^ low precision wastes context budget and degrades the answer");
        if (c5) passed++; else failed++;

        // 6. NDCG rewards putting the HIGHEST-graded document first.
        List<String> best = List.of("refund-policy", "refund-enterprise");
        List<String> swapped = List.of("refund-enterprise", "refund-policy");
        Map<String, Integer> grades = Map.of("refund-policy", 3, "refund-enterprise", 2);
        double nBest = ndcgAtK(best, grades, 5);
        double nSwapped = ndcgAtK(swapped, grades, 5);
        boolean c6 = Math.abs(nBest - 1.0) < 1e-9 && nSwapped < nBest;
        System.out.printf("NDCG ideal=%.3f > swapped=%.3f                   : %s%n", nBest, nSwapped, c6);
        if (c6) passed++; else failed++;

        // 7. An empty relevance set is vacuously perfect, not a zero.
        boolean c7 = recallAtK(List.of("a", "b"), new LinkedHashSet<>(), 5) == 1.0;
        System.out.println("no relevant docs exist -> recall 1.0 (vacuous)    : " + c7);
        if (c7) passed++; else failed++;

        // 8. Suite-level MRR.
        double mrr = meanReciprocalRank(cases);
        boolean c8 = Math.abs(mrr - (1.0 + 0.25 + 0.0) / 3) < 1e-9;
        System.out.printf("suite MRR = %.4f                                 : %s%n", mrr, c8);
        if (c8) passed++; else failed++;

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: retrieval metrics computed correctly and separate rank from presence."
                : "FAIL: retrieval metric mismatch.");
    }
}
