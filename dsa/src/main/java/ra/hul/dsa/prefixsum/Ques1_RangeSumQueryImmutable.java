package ra.hul.dsa.prefixsum;

/**
 * Range Sum Query Immutable - answer many inclusive range-sum queries after an O(n) prefix build.
 *
 * Time: O(n) build, O(1) per query. Space: O(n)
 */
public class Ques1_RangeSumQueryImmutable {

    static class NumArray {
        private final long[] prefix; // prefix[i] = sum of nums[0..i-1]; prefix[0] = 0 (sentinel)

        NumArray(int[] nums) {
            prefix = new long[nums.length + 1];
            for (int i = 0; i < nums.length; i++) {
                prefix[i + 1] = prefix[i] + nums[i];
            }
        }

        int sumRange(int left, int right) {
            // Everything up to and including `right`, minus everything before `left`.
            return (int) (prefix[right + 1] - prefix[left]);
        }
    }

    static void main() {
        NumArray na = new NumArray(new int[]{-2, 0, 3, -5, 2, -1});
        System.out.println(na.sumRange(0, 2)); // 1
        System.out.println(na.sumRange(2, 5)); // -1
        System.out.println(na.sumRange(0, 5)); // -3
    }
}
