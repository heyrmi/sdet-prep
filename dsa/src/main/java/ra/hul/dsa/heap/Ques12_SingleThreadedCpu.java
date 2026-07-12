package ra.hul.dsa.heap;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Single-Threaded CPU - event-driven scheduling: sort by arrival, min-heap by (processingTime, index).
 * LeetCode #1834 (Medium)
 *
 * Time: O(n log n), Space: O(n)
 */
public class Ques12_SingleThreadedCpu {

    public static int[] getOrder(int[][] tasks) {
        int n = tasks.length;

        // entry = {enqueueTime, processingTime, originalIndex}, sorted by enqueueTime.
        int[][] sorted = new int[n][3];
        for (int i = 0; i < n; i++) {
            sorted[i][0] = tasks[i][0];
            sorted[i][1] = tasks[i][1];
            sorted[i][2] = i;
        }
        Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));

        // Available tasks: smallest processingTime first, then smallest index.
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) ->
                a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[2], b[2]));

        int[] result = new int[n];
        long time = 0;
        int i = 0, r = 0;
        while (i < n || !heap.isEmpty()) {
            // Release everything that has arrived by 'time'.
            while (i < n && sorted[i][0] <= time) {
                heap.offer(new int[]{sorted[i][0], sorted[i][1], sorted[i][2]});
                i++;
            }
            if (heap.isEmpty()) {
                time = sorted[i][0]; // idle -> jump the clock to the next arrival
                continue;
            }
            int[] t = heap.poll();
            time += t[1];            // run to completion (long avoids overflow)
            result[r++] = t[2];
        }
        return result;
    }

    static void main() {
        System.out.println(Arrays.toString(
                getOrder(new int[][]{{1, 2}, {2, 4}, {3, 2}, {4, 1}}))); // [0, 2, 3, 1]
    }
}
