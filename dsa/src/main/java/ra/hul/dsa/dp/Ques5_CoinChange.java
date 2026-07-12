package ra.hul.dsa.dp;

import java.util.Arrays;

/**
 * Coin Change - Fewest coins (unlimited supply) that sum to amount, or -1 if impossible.
 *
 * Time: O(amount * #coins), Space: O(amount) - sentinel = amount + 1 (survives +1 without overflow).
 */
public class Ques5_CoinChange {

    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);   // "unreachable" sentinel, safe to add 1 to
        dp[0] = 0;                     // zero coins make amount 0
        for (int a = 1; a <= amount; a++) {
            for (int c : coins) {
                if (c <= a) {          // guard: coin may exceed current amount
                    dp[a] = Math.min(dp[a], dp[a - c] + 1);
                }
            }
        }
        return dp[amount] > amount ? -1 : dp[amount];
    }

    static void main() {
        System.out.println(coinChange(new int[]{1, 2, 5}, 11)); // 3
        System.out.println(coinChange(new int[]{2}, 3));        // -1
        System.out.println(coinChange(new int[]{1}, 0));        // 0
    }
}
