package ra.hul.dsa.dpadvanced;

/**
 * Target Sum - Number of +/- sign assignments over nums that sum to target.
 *
 * Time: O(n*S), Space: O(S) - reduces to counting subsets summing to S = (total + target) / 2.
 */
public class Ques2_TargetSum {

    public static int findTargetSumWays(int[] nums, int target) {
        int total = 0;
        for (int x : nums) total += x;

        // Guards: S must be a non-negative integer.
        if (Math.abs(target) > total) return 0;
        if (((total + target) & 1) == 1) return 0;
        int s = (total + target) / 2;

        int[] dp = new int[s + 1];     // dp[k] = number of subsets summing to k
        dp[0] = 1;                     // empty subset makes sum 0
        for (int x : nums) {
            for (int k = s; k >= x; k--) {   // BACKWARD: each number used at most once
                dp[k] += dp[k - x];
            }
        }
        return dp[s];
    }

    static void main() {
        System.out.println(findTargetSumWays(new int[]{1, 1, 1, 1, 1}, 3)); // 5
        System.out.println(findTargetSumWays(new int[]{1}, 1));             // 1
        System.out.println(findTargetSumWays(new int[]{1}, 2));             // 0
        System.out.println(findTargetSumWays(new int[]{0, 0, 0, 0, 0}, 0)); // 32
    }
}
