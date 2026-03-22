package ra.hul.dsa.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ques6_MergeIntervals {

    public static int[][] merge(int[][] intervals) {
        // Step 1: Sort based on the start time of the intervals
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        List<int[]> result = new ArrayList<>();

        // Add first interval to compare from
        result.add(intervals[0]);

        for (int i = 1; i < intervals.length; i++) {
            int[] lastAddedInterval = result.getLast();
            int[] currentInterval = intervals[i];

            // if currentInterval's start time is <= lastAddedInterval, merge them
            if (currentInterval[0] <= lastAddedInterval[1]) {
                // overlap => extend the end
                lastAddedInterval[1] = Math.max(currentInterval[1], lastAddedInterval[1]);
            } else {
                // no overlap => new interval
                result.add(currentInterval);
            }
        }
        return result.toArray(new int[result.size()][]);
    }

    static void main() {
        System.out.println(Arrays.deepToString(merge(new int[][]{{1,3},{2,6},{8,10},{15,18}})));  // [[1, 6], [8, 10], [15, 18]]
        System.out.println(Arrays.deepToString(merge(new int[][]{{1,4},{4,5}})));                 // [[1, 5]]
        System.out.println(Arrays.deepToString(merge(new int[][]{{1,4},{0,4}})));                 // [[0, 4]]
        System.out.println(Arrays.deepToString(merge(new int[][]{{1,4},{2,3}})));                 // [[1, 4]]
    }
}
