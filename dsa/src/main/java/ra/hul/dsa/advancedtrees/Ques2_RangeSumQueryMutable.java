package ra.hul.dsa.advancedtrees;

/**
 * Range Sum Query - Mutable - Fenwick tree where update is a SET applied as add-the-difference.
 * LeetCode #307 (Medium)
 *
 * Time: O(log n) per op, Space: O(n)
 */
public class Ques2_RangeSumQueryMutable {

    static class NumArray {
        private final int n;
        private final long[] tree; // 1-indexed Fenwick
        private final long[] cur;  // current value of each element

        NumArray(int[] nums) {
            this.n = nums.length;
            this.tree = new long[n + 1];
            this.cur = new long[n];
            for (int i = 0; i < n; i++) {
                cur[i] = nums[i];
                bitAdd(i, nums[i]);
            }
        }

        void update(int i, int val) {
            long delta = val - cur[i];
            cur[i] = val;
            bitAdd(i, delta);
        }

        long sumRange(int l, int r) {
            return prefixSum(r) - prefixSum(l - 1);
        }

        private void bitAdd(int i, long delta) {
            for (int j = i + 1; j <= n; j += j & -j) tree[j] += delta;
        }

        private long prefixSum(int i) {
            long s = 0;
            for (int j = i + 1; j > 0; j -= j & -j) s += tree[j];
            return s;
        }
    }

    static void main() {
        NumArray na = new NumArray(new int[]{1, 3, 5});
        System.out.println(na.sumRange(0, 2)); // 9
        na.update(1, 2);                       // nums = [1, 2, 5]
        System.out.println(na.sumRange(0, 2)); // 8
    }
}
