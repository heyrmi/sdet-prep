package ra.hul.dsa.unionfind;

/**
 * Number of Connected Components - union every edge; count starts at n and falls on each merge.
 * LeetCode #323 (Medium)
 *
 * Time: O((n + E) * alpha(n)) ~ O(n + E), Space: O(n)
 */
public class Ques4_NumberOfConnectedComponents {

    static int countComponents(int n, int[][] edges) {
        UnionFind uf = new UnionFind(n);
        for (int[] e : edges) uf.union(e[0], e[1]);
        return uf.count();
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

        public int count() {
            return count;
        }
    }

    static void main() {
        System.out.println(countComponents(5, new int[][]{{0, 1}, {1, 2}, {3, 4}}));         // 2
        System.out.println(countComponents(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}})); // 1
        System.out.println(countComponents(4, new int[][]{}));                               // 4
    }
}
