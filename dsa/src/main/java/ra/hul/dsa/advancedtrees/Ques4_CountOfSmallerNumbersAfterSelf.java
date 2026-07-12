package ra.hul.dsa.advancedtrees;

import java.util.Arrays;
import java.util.List;

/**
 * Count of Smaller Numbers After Self - coordinate-compress values, sweep right->left with a Fenwick counter.
 * LeetCode #315 (Hard)
 *
 * Time: O(n log n), Space: O(n)
 */
public class Ques4_CountOfSmallerNumbersAfterSelf {

    static List<Integer> countSmaller(int[] nums) {
        int n = nums.length;

        // 1. Coordinate compression: sorted distinct values -> rank.
        int[] sorted = nums.clone();
        Arrays.sort(sorted);
        int k = 0;
        int[] distinct = new int[n];
        for (int i = 0; i < n; i++) {
            if (i == 0 || sorted[i] != sorted[i - 1]) distinct[k++] = sorted[i];
        }
        // distinct[0..k-1] are the unique sorted values; rank = index via binary search.

        // 2. Sweep right -> left.
        Fenwick fen = new Fenwick(k);
        Integer[] counts = new Integer[n];
        for (int i = n - 1; i >= 0; i--) {
            int rank = lowerBound(distinct, k, nums[i]); // index of nums[i] in distinct
            counts[i] = fen.prefixCount(rank - 1);       // strictly-smaller values seen so far
            fen.add(rank, 1);                            // record nums[i] as seen
        }
        return Arrays.asList(counts);
    }

    /** First index in distinct[0..k-1] equal to target (target is guaranteed present). */
    private static int lowerBound(int[] distinct, int k, int target) {
        int lo = 0, hi = k - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (distinct[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Fenwick tree as a frequency counter. 0-indexed public API; 1-indexed internally. */
    static class Fenwick {
        private final int n;
        private final int[] tree; // 1-indexed, length n+1

        Fenwick(int n) {
            this.n = n;
            this.tree = new int[n + 1];
        }

        void add(int i, int delta) {
            for (int j = i + 1; j <= n; j += j & -j) tree[j] += delta;
        }

        int prefixCount(int i) {
            int s = 0;
            for (int j = i + 1; j > 0; j -= j & -j) s += tree[j];
            return s; // prefixCount(-1): j starts at 0, loop never runs, returns 0
        }
    }

    static void main() {
        System.out.println(countSmaller(new int[]{5, 2, 6, 1})); // [2, 1, 1, 0]
        System.out.println(countSmaller(new int[]{-1}));         // [0]
        System.out.println(countSmaller(new int[]{-1, -1}));     // [0, 0]
    }
}
