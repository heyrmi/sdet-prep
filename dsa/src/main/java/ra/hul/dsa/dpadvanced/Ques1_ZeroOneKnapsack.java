package ra.hul.dsa.dpadvanced;

/**
 * 0/1 Knapsack - Max total value of items (each taken at most once) fitting a weight capacity.
 *
 * Time: O(n*capacity), Space: O(capacity) - rolling 1D array, capacity loop runs BACKWARD.
 */
public class Ques1_ZeroOneKnapsack {

    public static int knapsack(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];          // dp[c] = best value for capacity c
        for (int i = 0; i < weights.length; i++) {
            int w = weights[i], v = values[i];
            for (int c = capacity; c >= w; c--) {  // BACKWARD: each dp[c-w] still excludes item i
                dp[c] = Math.max(dp[c], dp[c - w] + v);
            }
        }
        return dp[capacity];
    }

    static void main() {
        System.out.println(knapsack(new int[]{1, 3, 4, 5}, new int[]{1, 4, 5, 7}, 7)); // 9
        System.out.println(knapsack(new int[]{2, 2, 3}, new int[]{3, 4, 5}, 5));       // 9
        System.out.println(knapsack(new int[]{5}, new int[]{10}, 4));                  // 0
    }
}
