package ra.hul.dsa.unionfind;

/**
 * Implement Union-Find (Disjoint Set Union) - path compression (path-halving) + union by rank.
 *
 * Time: amortized O(alpha(n)) ~ O(1) per op, Space: O(n)
 */
public class Ques1_ImplementUnionFind {

    static class UnionFind {
        private final int[] parent; // parent[i] = parent of i; root iff parent[i] == i
        private final int[] rank;   // upper bound on tree height, used to keep trees shallow
        private int count;          // number of disjoint sets

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i; // each element is its own root
            count = n;
        }

        public int find(int x) {
            // Iterative path-halving: point each node at its grandparent on the way up.
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        public void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA == rootB) return; // already connected; count unchanged

            // Union by rank: hang the shorter tree under the taller one.
            if (rank[rootA] < rank[rootB]) {
                parent[rootA] = rootB;
            } else if (rank[rootA] > rank[rootB]) {
                parent[rootB] = rootA;
            } else {
                parent[rootB] = rootA;
                rank[rootA]++;
            }
            count--;
        }

        public boolean connected(int a, int b) {
            return find(a) == find(b);
        }

        public int count() {
            return count;
        }
    }

    static void main() {
        UnionFind uf = new UnionFind(5);       // {0} {1} {2} {3} {4}, count = 5
        uf.union(0, 1);                        // {0,1} {2} {3} {4},   count = 4
        uf.union(1, 2);                        // {0,1,2} {3} {4},     count = 3
        System.out.println(uf.connected(0, 2)); // true
        System.out.println(uf.connected(0, 3)); // false
        uf.union(0, 2);                        // already same set; count stays 3
        System.out.println(uf.count());        // 3
    }
}
