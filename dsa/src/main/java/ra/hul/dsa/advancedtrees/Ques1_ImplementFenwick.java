package ra.hul.dsa.advancedtrees;

/**
 * Implement a Fenwick Tree (Binary Indexed Tree) - point update + prefix/range sum via lowbit loops.
 *
 * Time: O(log n) per op, Space: O(n)
 */
public class Ques1_ImplementFenwick {

    /** Public API 0-indexed; internal array 1-indexed (cell 0 unused). */
    static class Fenwick {
        private final int n;
        private final long[] tree; // 1-indexed, length n+1

        Fenwick(int n) {
            this.n = n;
            this.tree = new long[n + 1];
        }

        void update(int i, long delta) {
            for (int j = i + 1; j <= n; j += j & -j) {
                tree[j] += delta;
            }
        }

        long prefixSum(int i) {
            long sum = 0;
            for (int j = i + 1; j > 0; j -= j & -j) {
                sum += tree[j];
            }
            return sum; // prefixSum(-1): j starts at 0, loop never runs, returns 0
        }

        long rangeSum(int l, int r) {
            return prefixSum(r) - prefixSum(l - 1);
        }
    }

    static void main() {
        Fenwick f = new Fenwick(5); // [0,0,0,0,0]
        f.update(0, 3);             // [3,0,0,0,0]
        f.update(2, 5);             // [3,0,5,0,0]
        System.out.println(f.prefixSum(2));   // 8
        System.out.println(f.rangeSum(1, 3)); // 5
        f.update(2, -5);            // [3,0,0,0,0]
        System.out.println(f.prefixSum(2));   // 3
    }
}
