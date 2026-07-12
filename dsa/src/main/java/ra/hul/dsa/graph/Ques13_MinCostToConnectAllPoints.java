package ra.hul.dsa.graph;

import java.util.PriorityQueue;

/**
 * Min Cost to Connect All Points - MST over the complete graph with Manhattan edge weights (Prim).
 * LeetCode #1584 (Medium)
 *
 * Time: O(n^2 log n), Space: O(n)
 */
public class Ques13_MinCostToConnectAllPoints {

    public static int minCostConnectPoints(int[][] points) {
        int n = points.length;
        if (n <= 1) return 0;

        boolean[] inTree = new boolean[n];
        // Min-heap of {cost, point}; compare by cost with Integer.compare (no overflow).
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        pq.add(new int[]{0, 0});            // start at point 0 with cost 0

        long total = 0;                     // sum can exceed int → use long
        int added = 0;

        while (!pq.isEmpty() && added < n) {
            int[] top = pq.poll();
            int cost = top[0], p = top[1];
            if (inTree[p]) continue;        // stale entry — already connected

            inTree[p] = true;
            total += cost;
            added++;

            // Push edges from p to every point not yet in the tree.
            for (int q = 0; q < n; q++) {
                if (!inTree[q]) {
                    int d = Math.abs(points[p][0] - points[q][0])
                          + Math.abs(points[p][1] - points[q][1]);
                    pq.add(new int[]{d, q});
                }
            }
        }

        return (int) total;
    }

    static void main() {
        System.out.println(minCostConnectPoints(
            new int[][]{{0, 0}, {2, 2}, {3, 10}, {5, 2}, {7, 0}}));   // 20
        System.out.println(minCostConnectPoints(
            new int[][]{{3, 12}, {-2, 5}, {-4, 1}}));                 // 18
        System.out.println(minCostConnectPoints(new int[][]{{0, 0}})); // 0
    }
}
