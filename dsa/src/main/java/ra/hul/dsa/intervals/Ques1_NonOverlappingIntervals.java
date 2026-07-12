package ra.hul.dsa.intervals;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Non-overlapping Intervals - minimum number of intervals to remove so the rest don't overlap.
 *
 * Time: O(n log n), Space: O(1) - greedy, sort by end, keep the earliest-finishing interval
 */
public class Ques1_NonOverlappingIntervals {

    public static int eraseOverlapIntervals(int[][] intervals) {
        // Sort by END. comparingInt is overflow-safe.
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[1]));

        int removed = 0;
        long prevEnd = Long.MIN_VALUE;  // end of the last interval we kept
        for (int[] cur : intervals) {
            if (cur[0] >= prevEnd) {
                prevEnd = cur[1];       // no overlap -> keep it
            } else {
                removed++;              // overlaps the kept one -> remove this one
            }
        }
        return removed;
    }

    static void main() {
        System.out.println(eraseOverlapIntervals(new int[][]{{1, 2}, {2, 3}, {3, 4}, {1, 3}})); // 1
        System.out.println(eraseOverlapIntervals(new int[][]{{1, 2}, {1, 2}, {1, 2}}));          // 2
        System.out.println(eraseOverlapIntervals(new int[][]{{1, 2}, {2, 3}}));                  // 0
    }
}
