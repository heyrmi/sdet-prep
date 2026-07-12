package ra.hul.dsa.dpadvanced;

/**
 * Burst Balloons - Max coins from bursting all balloons (coins = left * i * right).
 *
 * Time: O(n^3), Space: O(n^2) - interval DP; dp[i][j] decided by which balloon is popped LAST in (i, j).
 */
public class Ques6_BurstBalloons {

    public static int maxCoins(int[] nums) {
        int n = nums.length;
        int m = n + 2;
        int[] arr = new int[m];               // pad with 1s on both ends
        arr[0] = 1;
        arr[m - 1] = 1;
        for (int i = 0; i < n; i++) arr[i + 1] = nums[i];

        int[][] dp = new int[m][m];           // dp[i][j] = coins from balloons strictly in (i, j)

        for (int len = 2; len < m; len++) {            // interval span (boundary distance)
            for (int i = 0; i + len < m; i++) {
                int j = i + len;
                for (int k = i + 1; k < j; k++) {      // k = LAST balloon popped in (i, j)
                    int coins = dp[i][k] + dp[k][j] + arr[i] * arr[k] * arr[j];
                    if (coins > dp[i][j]) dp[i][j] = coins;
                }
            }
        }
        return dp[0][m - 1];
    }

    static void main() {
        System.out.println(maxCoins(new int[]{3, 1, 5, 8})); // 167
        System.out.println(maxCoins(new int[]{1, 5}));       // 10
        System.out.println(maxCoins(new int[]{}));           // 0
        System.out.println(maxCoins(new int[]{7}));          // 7
    }
}
