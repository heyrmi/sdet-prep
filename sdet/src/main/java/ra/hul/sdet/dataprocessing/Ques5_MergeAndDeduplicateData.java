package ra.hul.sdet.dataprocessing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge and Deduplicate Data from Multiple Sources - Combine records from two sources on a
 * common key, resolving duplicates with a "keep latest" rule, and report merge statistics.
 * Common SDET question: "Two feeds have overlapping records keyed by id; produce one clean set
 * keeping the newest version of each, and report totals/merged/duplicates-removed."
 *
 * Self-contained: both sources are built inline as POJO lists (NO files, NO network, NO DB).
 */
public class Ques5_MergeAndDeduplicateData {

    /** Common data model both sources map into. updatedAt drives the keep-latest rule. */
    record Record(String id, String name, double amount, Instant updatedAt) {}

    record MergeResult(List<Record> merged, int totalInput, int duplicatesRemoved, int conflictsResolved) {}

    /**
     * Merge many sources keyed by Record.id, keeping the record with the newest updatedAt.
     * Later-source ties (equal timestamp) are treated as duplicates and the incoming one wins
     * only if strictly newer, so processing order is deterministic.
     */
    @SafeVarargs
    static MergeResult mergeKeepLatest(List<Record>... sources) {
        Map<String, Record> byKey = new LinkedHashMap<>();
        int total = 0;
        int duplicatesRemoved = 0;
        int conflictsResolved = 0;
        for (List<Record> source : sources) {
            for (Record incoming : source) {
                total++;
                Record existing = byKey.get(incoming.id());
                if (existing == null) {
                    byKey.put(incoming.id(), incoming);
                } else {
                    duplicatesRemoved++; // a second record for the same key was seen
                    if (incoming.updatedAt().isAfter(existing.updatedAt())) {
                        byKey.put(incoming.id(), incoming); // newer wins
                        conflictsResolved++;
                    }
                    // else: keep existing (it is newer or equal)
                }
            }
        }
        return new MergeResult(new ArrayList<>(byKey.values()), total, duplicatesRemoved, conflictsResolved);
    }

    static void main() {
        Instant t0 = Instant.parse("2026-07-01T00:00:00Z");

        // Source A (e.g. from CSV feed)
        List<Record> sourceA = List.of(
                new Record("u1", "Alice", 100.0, t0),
                new Record("u2", "Bob", 50.0, t0.plusSeconds(60)),
                new Record("u3", "Carol", 75.0, t0)
        );

        // Source B (e.g. from JSON feed) — overlaps u1 (newer) and u2 (older), adds u4
        List<Record> sourceB = List.of(
                new Record("u1", "Alice Smith", 120.0, t0.plusSeconds(3600)), // newer -> should win
                new Record("u2", "Bobby", 40.0, t0),                          // older -> should be dropped
                new Record("u4", "Dave", 200.0, t0.plusSeconds(120))          // new key
        );

        MergeResult result = mergeKeepLatest(sourceA, sourceB);

        System.out.println("Merged records:");
        result.merged().forEach(r -> System.out.printf("  %-4s %-12s $%.2f  @%s%n",
                r.id(), r.name(), r.amount(), r.updatedAt()));

        System.out.println("\nMerge statistics:");
        System.out.println("  total input records : " + result.totalInput());
        System.out.println("  unique keys (output): " + result.merged().size());
        System.out.println("  duplicates removed  : " + result.duplicatesRemoved());
        System.out.println("  conflicts resolved  : " + result.conflictsResolved());

        // Verify
        Map<String, Record> out = new LinkedHashMap<>();
        result.merged().forEach(r -> out.put(r.id(), r));

        check("output has 4 unique keys", result.merged().size() == 4);
        check("total input counted", result.totalInput() == 6);
        check("2 duplicate keys detected", result.duplicatesRemoved() == 2);
        check("u1 keeps NEWER version from B", "Alice Smith".equals(out.get("u1").name())
                && out.get("u1").amount() == 120.0);
        check("u2 keeps NEWER version from A", "Bob".equals(out.get("u2").name())
                && out.get("u2").amount() == 50.0);
        check("u3 (no dup) preserved", out.containsKey("u3"));
        check("u4 (new key) added", out.containsKey("u4"));
        check("only 1 real conflict flipped the value", result.conflictsResolved() == 1);

        System.out.println("\nPASSED: sources merged on key with keep-latest dedupe and correct stats.");
    }

    static void check(String label, boolean ok) {
        System.out.println((ok ? "  PASS: " : "  FAIL: ") + label);
        if (!ok) throw new AssertionError("Check failed: " + label);
    }
}
