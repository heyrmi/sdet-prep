package ra.hul.dsa.dp;

/**
 * House Robber II - Houses in a circle: first and last are adjacent.
 *
 * Time: O(n), Space: O(1) - run linear House Robber twice: skip last, skip first, take the max.
 */
public class Ques3_HouseRobberII {

    public static int rob(int[] nums) {
        int n = nums.length;
        if (n == 1) return nums[0];
        // Skip last (0..n-2) vs skip first (1..n-1); both break the circle.
        return Math.max(robLine(nums, 0, n - 2), robLine(nums, 1, n - 1));
    }

    // Linear House Robber over nums[start..end] inclusive.
    static int robLine(int[] nums, int start, int end) {
        int prev2 = 0, prev1 = 0;
        for (int i = start; i <= end; i++) {
            int cur = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static void main() {
        System.out.println(rob(new int[]{2, 3, 2}));    // 3
        System.out.println(rob(new int[]{1, 2, 3, 1})); // 4
        System.out.println(rob(new int[]{1, 2, 3}));    // 3
    }
}
