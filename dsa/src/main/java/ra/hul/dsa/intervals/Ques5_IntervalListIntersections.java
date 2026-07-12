package ra.hul.dsa.intervals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Interval List Intersections - intersection of two already-sorted, disjoint interval lists.
 *
 * Time: O(m + n), Space: O(m + n) - two pointers, no sort needed
 */
public class Ques5_IntervalListIntersections {

    public static int[][] intervalIntersection(int[][] firstList, int[][] secondList) {
        List<int[]> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < firstList.length && j < secondList.length) {
            int lo = Math.max(firstList[i][0], secondList[j][0]);
            int hi = Math.min(firstList[i][1], secondList[j][1]);
            if (lo <= hi) {                 // <= : single-point overlaps like [2,2] count
                result.add(new int[]{lo, hi});
            }
            // Advance the interval that ends first — it can't overlap anything later.
            if (firstList[i][1] < secondList[j][1]) {
                i++;
            } else {
                j++;
            }
        }
        return result.toArray(new int[0][]);
    }

    static void main() {
        int[][] a = {{0, 2}, {5, 10}, {13, 23}, {24, 25}};
        int[][] b = {{1, 5}, {8, 12}, {15, 24}, {25, 26}};
        System.out.println(Arrays.deepToString(intervalIntersection(a, b)));
        // [[1, 2], [5, 5], [8, 10], [15, 23], [24, 24], [25, 25]]
        System.out.println(Arrays.deepToString(intervalIntersection(new int[][]{{1, 3}, {5, 9}}, new int[][]{}))); // []
    }
}
