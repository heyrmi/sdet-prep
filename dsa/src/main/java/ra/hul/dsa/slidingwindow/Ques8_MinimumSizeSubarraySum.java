package ra.hul.dsa.slidingwindow;

/**
 * Minimum Size Subarray Sum - minimal length of a contiguous subarray with sum >= target (positive nums).
 *
 * Time: O(n), Space: O(1)
 */
public class Ques8_MinimumSizeSubarraySum {

    public static int minSubArrayLen(int target, int[] nums) {
        int left = 0, best = Integer.MAX_VALUE;
        long sum = 0; // long is the robust choice; sums can approach 1e9
        for (int right = 0; right < nums.length; right++) {
            sum += nums[right];                 // expand
            while (sum >= target) {             // valid window -> shrink to find the tightest
                best = Math.min(best, right - left + 1);
                sum -= nums[left];
                left++;
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    static void main() {
        System.out.println(minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));           // 2
        System.out.println(minSubArrayLen(4, new int[]{1, 4, 4}));                     // 1
        System.out.println(minSubArrayLen(11, new int[]{1, 1, 1, 1, 1, 1, 1, 1}));     // 0
    }
}
