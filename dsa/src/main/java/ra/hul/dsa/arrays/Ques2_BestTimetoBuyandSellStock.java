package ra.hul.dsa.arrays;

public class Ques2_BestTimetoBuyandSellStock {

    public static int maxProfit(int[] prices) {
        int maxProfit = 0;
        int lowestPrice = Integer.MAX_VALUE;

        for (int price : prices) {
            if (price < lowestPrice) {
                lowestPrice = price;
            } else {
                maxProfit = Math.max(maxProfit, price - lowestPrice);
            }
        }

        return maxProfit;
    }

    static void main() {
        System.out.println(maxProfit(new int[]{7, 1, 5, 3, 6, 4}));  // 5
        System.out.println(maxProfit(new int[]{7, 6, 4, 3, 1}));     // 0
        System.out.println(maxProfit(new int[]{2, 4, 1}));           // 2
    }
}
