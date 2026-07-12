package ra.hul.dsa.arrays;

/**
 * Maximum Product Subarray - Find the contiguous subarray with the largest product.
 * LeetCode #152 (Medium)
 *
 * Track BOTH the max and min product ending at each index: a negative number flips them,
 * so today's min (a large negative) can become tomorrow's max when multiplied by a negative.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques10_MaximumProductSubarray {

    public static int maxProduct(int[] nums) {
        int max = nums[0], min = nums[0], result = nums[0];
        for (int i = 1; i < nums.length; i++) {
            int n = nums[i];
            if (n < 0) {           // swap: multiplying by a negative flips max/min roles
                int tmp = max;
                max = min;
                min = tmp;
            }
            max = Math.max(n, max * n);
            min = Math.min(n, min * n);
            result = Math.max(result, max);
        }
        return result;
    }

    static void main() {
        System.out.println(maxProduct(new int[]{2, 3, -2, 4}));      // 6   ([2,3])
        System.out.println(maxProduct(new int[]{-2, 0, -1}));        // 0
        System.out.println(maxProduct(new int[]{-2, 3, -4}));        // 24  ([-2,3,-4])
        System.out.println(maxProduct(new int[]{-2}));               // -2
    }
}
