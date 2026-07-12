package ra.hul.dsa.dp;

import java.util.Arrays;

/**
 * Longest Increasing Subsequence - Length of the longest strictly increasing subsequence.
 *
 * Time: O(n^2), Space: O(n) - dp[i] = longest increasing subsequence ending at i; answer = max(dp).
 */
public class Ques7_LongestIncreasingSubsequence {

    public static int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n];
        Arrays.fill(dp, 1);            // each element alone is length 1
        int best = 1;
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {           // strictly increasing
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    static void main() {
        System.out.println(lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18})); // 4
        System.out.println(lengthOfLIS(new int[]{0, 1, 0, 3, 2, 3}));           // 4
        System.out.println(lengthOfLIS(new int[]{7, 7, 7, 7}));                 // 1
    }
}
