package ra.hul.dsa.greedy;

/**
 * Best Time to Buy and Sell Stock - Find max profit from one buy-sell transaction.
 * LeetCode #121 (Easy)
 *
 * Time: O(n), Space: O(1) - track min price seen so far
 */
public class Ques1_BestTimeToBuyAndSellStock {

    public static int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int maxProfit = 0;
        for (int price : prices) {
            minPrice = Math.min(minPrice, price);
            maxProfit = Math.max(maxProfit, price - minPrice);
        }
        return maxProfit;
    }

    public static void main(String[] args) {
        System.out.println(maxProfit(new int[]{7, 1, 5, 3, 6, 4})); // 5 (buy@1, sell@6)
        System.out.println(maxProfit(new int[]{7, 6, 4, 3, 1}));    // 0 (no profit possible)
    }
}
