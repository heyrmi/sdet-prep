package ra.hul.dsa.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Insert Interval - Given a sorted list of non-overlapping intervals, insert a new interval
 * and merge if necessary. Return the resulting sorted, non-overlapping intervals.
 * LeetCode #57 (Medium)
 *
 * Time: O(n), Space: O(n) - single linear pass in three phases
 */
public class Ques9_InsertInterval {

    public static int[][] insert(int[][] intervals, int[] newInterval) {
        List<int[]> result = new ArrayList<>();
        int i = 0, n = intervals.length;

        // 1. Intervals that end before newInterval starts — copy as-is.
        while (i < n && intervals[i][1] < newInterval[0]) {
            result.add(intervals[i++]);
        }

        // 2. Overlapping intervals — merge them into newInterval.
        while (i < n && intervals[i][0] <= newInterval[1]) {
            newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
            newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
            i++;
        }
        result.add(newInterval);

        // 3. Remaining intervals that start after newInterval ends.
        while (i < n) {
            result.add(intervals[i++]);
        }

        return result.toArray(new int[result.size()][]);
    }

    static void main() {
        System.out.println(Arrays.deepToString(insert(new int[][]{{1, 3}, {6, 9}}, new int[]{2, 5})));
        // [[1, 5], [6, 9]]
        System.out.println(Arrays.deepToString(insert(new int[][]{{1, 2}, {3, 5}, {6, 7}, {8, 10}, {12, 16}}, new int[]{4, 8})));
        // [[1, 2], [3, 10], [12, 16]]
        System.out.println(Arrays.deepToString(insert(new int[][]{}, new int[]{5, 7})));  // [[5, 7]]
    }
}
