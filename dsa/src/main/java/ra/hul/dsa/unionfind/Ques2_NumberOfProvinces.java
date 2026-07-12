package ra.hul.dsa.unionfind;

/**
 * Number of Provinces - union every connected pair, then the province count is the DSU component count.
 * LeetCode #547 (Medium)
 *
 * Time: O(n^2 * alpha(n)) ~ O(n^2), Space: O(n)
 */
public class Ques2_NumberOfProvinces {

    static int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        UnionFind uf = new UnionFind(n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) { // upper triangle only (symmetric matrix)
                if (isConnected[i][j] == 1) uf.union(i, j);
            }
        }
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
        System.out.println(findCircleNum(new int[][]{
            {1, 1, 0},
            {1, 1, 0},
            {0, 0, 1}
        })); // 2
        System.out.println(findCircleNum(new int[][]{
            {1, 0, 0},
            {0, 1, 0},
            {0, 0, 1}
        })); // 3
    }
}
