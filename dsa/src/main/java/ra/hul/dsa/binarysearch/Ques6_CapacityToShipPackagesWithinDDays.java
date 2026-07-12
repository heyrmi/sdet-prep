package ra.hul.dsa.binarysearch;

/**
 * Capacity to Ship Packages Within D Days - binary search on the answer (least ship capacity).
 * LeetCode #1011 (Medium)
 *
 * Time: O(n log(sum)), Space: O(1)
 */
public class Ques6_CapacityToShipPackagesWithinDDays {

    public static int shipWithinDays(int[] weights, int days) {
        long lo = 0, hi = 0;
        for (int w : weights) { lo = Math.max(lo, w); hi += w; }  // [max, sum]
        while (lo < hi) {
            long mid = lo + (hi - lo) / 2;
            if (feasible(weights, mid, days)) hi = mid;           // try smaller capacity
            else lo = mid + 1;
        }
        return (int) lo;
    }

    /**
     * True if packages (in order) ship within `days` days using capacity `cap`.
     * Greedy: fill each day until the next package would overflow, then start a new day.
     */
    static boolean feasible(int[] weights, long cap, int days) {
        int used = 1;
        long load = 0;
        for (int w : weights) {
            if (load + w > cap) { used++; load = 0; }   // start a new day
            load += w;
        }
        return used <= days;
    }

    static void main() {
        System.out.println(shipWithinDays(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 5)); // 15
        System.out.println(shipWithinDays(new int[]{3, 2, 2, 4, 1, 4}, 3));              // 6
        System.out.println(shipWithinDays(new int[]{1, 2, 3, 1, 1}, 4));                 // 3
    }
}
