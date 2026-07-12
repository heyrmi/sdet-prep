package ra.hul.dsa.advancedtrees;

/**
 * Implement a Segment Tree (Range Minimum) - array-backed range-min tree with point updates.
 *
 * Time: build O(n), update/query O(log n), Space: O(n)
 */
public class Ques3_ImplementSegmentTree {

    /** Array-backed (root at 1, children 2k / 2k+1), sized 4*n. */
    static class SegmentTree {
        private final int n;
        private final int[] tree; // size 4*n, min of each node's range

        SegmentTree(int[] nums) {
            this.n = nums.length;
            this.tree = new int[4 * n];
            build(nums, 1, 0, n - 1);
        }

        private void build(int[] nums, int node, int lo, int hi) {
            if (lo == hi) {
                tree[node] = nums[lo];
                return;
            }
            int mid = lo + (hi - lo) / 2;
            build(nums, 2 * node, lo, mid);
            build(nums, 2 * node + 1, mid + 1, hi);
            tree[node] = Math.min(tree[2 * node], tree[2 * node + 1]);
        }

        void update(int i, int val) {
            update(1, 0, n - 1, i, val);
        }

        private void update(int node, int lo, int hi, int i, int val) {
            if (lo == hi) {
                tree[node] = val;
                return;
            }
            int mid = lo + (hi - lo) / 2;
            if (i <= mid) update(2 * node, lo, mid, i, val);
            else update(2 * node + 1, mid + 1, hi, i, val);
            tree[node] = Math.min(tree[2 * node], tree[2 * node + 1]);
        }

        int queryMin(int l, int r) {
            return query(1, 0, n - 1, l, r);
        }

        private int query(int node, int lo, int hi, int l, int r) {
            if (r < lo || hi < l) return Integer.MAX_VALUE; // no overlap -> identity for min
            if (l <= lo && hi <= r) return tree[node];       // total overlap
            int mid = lo + (hi - lo) / 2;
            return Math.min(query(2 * node, lo, mid, l, r),
                            query(2 * node + 1, mid + 1, hi, l, r));
        }
    }

    static void main() {
        SegmentTree st = new SegmentTree(new int[]{2, 5, 1, 4, 9, 3});
        System.out.println(st.queryMin(0, 5)); // 1
        System.out.println(st.queryMin(3, 5)); // 3
        st.update(2, 7);                       // [2, 5, 7, 4, 9, 3]
        System.out.println(st.queryMin(0, 5)); // 2
        System.out.println(st.queryMin(1, 2)); // 5
    }
}
