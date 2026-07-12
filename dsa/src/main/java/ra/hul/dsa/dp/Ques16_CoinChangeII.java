package ra.hul.dsa.dp;

/**
 * Coin Change II - Number of combinations (unlimited supply) that make up amount.
 *
 * Time: O(K*amount), Space: O(K*amount) - dp[k][a] = combos for amount a using the first k coins.
 */
public class Ques16_CoinChangeII {

    public static int change(int amount, int[] coins) {
        int k = coins.length;
        int[][] dp = new int[k + 1][amount + 1];
        for (int i = 0; i <= k; i++) dp[i][0] = 1; // one way to make 0: take nothing
        for (int i = 1; i <= k; i++) {
            int coin = coins[i - 1];
            for (int a = 1; a <= amount; a++) {
                dp[i][a] = dp[i - 1][a]; // don't use coin i
                if (a >= coin) {
                    dp[i][a] += dp[i][a - coin]; // use one more copy of coin i (same row = reusable)
                }
            }
        }
        return dp[k][amount];
    }

    static void main() {
        System.out.println(change(5, new int[]{1, 2, 5}));  // 4
        System.out.println(change(3, new int[]{2}));        // 0
        System.out.println(change(10, new int[]{10}));      // 1
        System.out.println(change(0, new int[]{1, 2}));     // 1
    }
}
