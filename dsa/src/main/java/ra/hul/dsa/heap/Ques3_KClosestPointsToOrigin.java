package ra.hul.dsa.heap;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * K Closest Points to Origin - return the k points closest to (0,0) using a size-k max-heap.
 * LeetCode #973 (Medium)
 *
 * Time: O(n log k), Space: O(k)
 */
public class Ques3_KClosestPointsToOrigin {

    public static int[][] kClosest(int[][] points, int k) {
        // Max-heap keyed on squared distance: the farthest of the current k candidates is on top,
        // so we can evict it in O(log k) when a closer point arrives.
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Long.compare(dist(b), dist(a)));
        for (int[] p : points) {
            pq.offer(p);
            if (pq.size() > k) pq.poll(); // drop the farthest survivor
        }
        int[][] result = new int[pq.size()][];
        int i = 0;
        for (int[] p : pq) result[i++] = p; // order doesn't matter
        return result;
    }

    // Squared distance to origin, as a long to avoid int overflow on large coordinates.
    private static long dist(int[] p) {
        return (long) p[0] * p[0] + (long) p[1] * p[1];
    }

    static void main() {
        // Sort the (any-order) result by distance for a deterministic, readable printout.
        int[][] a = kClosest(new int[][]{{1, 3}, {-2, 2}}, 1);
        Arrays.sort(a, (x, y) -> Long.compare(dist(x), dist(y)));
        System.out.println(Arrays.deepToString(a)); // [[-2, 2]]

        int[][] b = kClosest(new int[][]{{3, 3}, {5, -1}, {-2, 4}}, 2);
        Arrays.sort(b, (x, y) -> Long.compare(dist(x), dist(y)));
        System.out.println(Arrays.deepToString(b)); // [[3, 3], [-2, 4]]
    }
}
