package ra.hul.dsa.dp;

/**
 * Min Cost Climbing Stairs - Minimum cost to reach the top (past the last stair).
 *
 * Time: O(n), Space: O(1) - dp[i] = min(dp[i-1] + cost[i-1], dp[i-2] + cost[i-2]); answer is dp[n].
 */
public class Ques4_MinCostClimbingStairs {

    public static int minCostClimbingStairs(int[] cost) {
        int n = cost.length;
        int prev2 = 0;   // dp[i-2]
        int prev1 = 0;   // dp[i-1]
        for (int i = 2; i <= n; i++) {
            int cur = Math.min(prev1 + cost[i - 1], prev2 + cost[i - 2]);
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;    // dp[n] after the loop
    }

    static void main() {
        System.out.println(minCostClimbingStairs(new int[]{10, 15, 20}));                       // 15
        System.out.println(minCostClimbingStairs(new int[]{1, 100, 1, 1, 1, 100, 1, 1, 100, 1})); // 6
    }
}
