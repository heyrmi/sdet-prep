package ra.hul.dsa.unionfind;

import java.util.Arrays;

/**
 * Redundant Connection - the first edge whose endpoints are already connected closes the cycle.
 * LeetCode #684 (Medium)
 *
 * Time: O(n * alpha(n)) ~ O(n), Space: O(n)
 */
public class Ques3_RedundantConnection {

    static int[] findRedundantConnection(int[][] edges) {
        int n = edges.length; // nodes are labeled 1..n
        UnionFind uf = new UnionFind(n + 1); // 1-indexed; index 0 unused
        for (int[] e : edges) {
            if (uf.connected(e[0], e[1])) return e; // both endpoints already in one set -> cycle
            uf.union(e[0], e[1]);
        }
        return new int[]{}; // unreachable: problem guarantees exactly one redundant edge
    }

    static class UnionFind {
        private final int[] parent;
        private final int[] rank;
        private int count;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
            count = n;
        }

        public int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        public void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if (rank[ra] < rank[rb]) parent[ra] = rb;
            else if (rank[ra] > rank[rb]) parent[rb] = ra;
            else { parent[rb] = ra; rank[ra]++; }
            count--;
        }

        public boolean connected(int a, int b) {
            return find(a) == find(b);
        }
    }

    static void main() {
        System.out.println(Arrays.toString(findRedundantConnection(new int[][]{
            {1, 2}, {1, 3}, {2, 3}
        }))); // [2, 3]
        System.out.println(Arrays.toString(findRedundantConnection(new int[][]{
            {1, 2}, {2, 3}, {3, 4}, {1, 4}, {1, 5}
        }))); // [1, 4]
    }
}
