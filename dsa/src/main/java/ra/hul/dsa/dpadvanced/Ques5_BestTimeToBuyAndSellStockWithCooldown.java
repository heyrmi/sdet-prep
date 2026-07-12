package ra.hul.dsa.dpadvanced;

/**
 * Best Time to Buy and Sell Stock with Cooldown - Max profit with a 1-day cooldown after selling.
 *
 * Time: O(n), Space: O(1) - three rolling states: hold / sold / rest.
 */
public class Ques5_BestTimeToBuyAndSellStockWithCooldown {

    public static int maxProfit(int[] prices) {
        if (prices == null || prices.length <= 1) return 0;

        int hold = -prices[0];   // own a share
        int sold = 0;            // sold today (cooldown tomorrow)
        int rest = 0;            // own nothing, free to buy

        for (int i = 1; i < prices.length; i++) {
            int p = prices[i];
            int newHold = Math.max(hold, rest - p);   // keep holding, or buy out of REST
            int newSold = hold + p;                   // sell the held share today
            int newRest = Math.max(rest, sold);       // keep resting, or finish a cooldown
            hold = newHold;
            sold = newSold;
            rest = newRest;
        }
        return Math.max(sold, rest);   // never end while still holding
    }

    static void main() {
        System.out.println(maxProfit(new int[]{1, 2, 3, 0, 2})); // 3
        System.out.println(maxProfit(new int[]{1}));             // 0
        System.out.println(maxProfit(new int[]{}));              // 0
        System.out.println(maxProfit(new int[]{2, 1, 4}));       // 3
    }
}
