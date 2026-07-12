package ra.hul.dsa.graph;

import java.util.Arrays;

/**
 * Redundant Connection - the edge that closes a cycle when adding edges one at a time (Union-Find).
 * LeetCode #684 (Medium)
 *
 * Time: O(n * alpha(n)), Space: O(n)
 */
public class Ques14_RedundantConnection {

    private static int[] parent;
    private static int[] rank;

    public static int[] findRedundantConnection(int[][] edges) {
        int n = edges.length;               // n edges => n nodes (1..n)
        parent = new int[n + 1];
        rank = new int[n + 1];
        for (int i = 1; i <= n; i++) parent[i] = i;

        for (int[] e : edges) {
            if (!union(e[0], e[1])) {        // already connected => this edge closes a cycle
                return new int[]{e[0], e[1]};
            }
        }
        return new int[0];                  // unreachable: problem guarantees one redundant edge
    }

    private static int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];   // path compression (halving)
            x = parent[x];
        }
        return x;
    }

    // Returns false if a and b were already in the same set (a cycle-closing edge).
    private static boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) rank[ra]++;
        return true;
    }

    static void main() {
        System.out.println(Arrays.toString(
            findRedundantConnection(new int[][]{{1, 2}, {1, 3}, {2, 3}})));                       // [2, 3]
        System.out.println(Arrays.toString(
            findRedundantConnection(new int[][]{{1, 2}, {2, 3}, {3, 4}, {1, 4}, {1, 5}})));       // [1, 4]
    }
}
