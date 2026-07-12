package ra.hul.dsa.dp;

/**
 * House Robber - Max money robbing non-adjacent houses on a street.
 *
 * Time: O(n), Space: O(1) - two rolling variables, dp[i] = max(dp[i-1], dp[i-2] + nums[i]).
 */
public class Ques2_HouseRobber {

    public static int rob(int[] nums) {
        int prev2 = 0;   // dp[i-2]: best up to two houses back
        int prev1 = 0;   // dp[i-1]: best up to the previous house
        for (int x : nums) {
            int cur = Math.max(prev1, prev2 + x);  // skip vs rob this house
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static void main() {
        System.out.println(rob(new int[]{1, 2, 3, 1}));    // 4
        System.out.println(rob(new int[]{2, 7, 9, 3, 1})); // 12
        System.out.println(rob(new int[]{5}));             // 5
    }
}
