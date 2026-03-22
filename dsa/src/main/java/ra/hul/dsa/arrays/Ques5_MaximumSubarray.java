package ra.hul.dsa.arrays;

public class Ques5_MaximumSubarray {

    public static int maxSubArray(int[] nums) {
        int currentSum = 0;
        int maxSum = Integer.MIN_VALUE;

        for (int num : nums) {
            // Running sum
            currentSum += num;

            // maxSum till now
            maxSum = Math.max(maxSum, currentSum);

            // if currentSum drops below 0,
            // drop it and start again from next index
            if (currentSum < 0)
                currentSum = 0;
        }
        return maxSum;
    }

    static void main() {
        System.out.println(maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));    // 6
        System.out.println(maxSubArray(new int[]{1}));                                // 1
        System.out.println(maxSubArray(new int[]{-1, -2, -3}));                       // -1
        System.out.println(maxSubArray(new int[]{5, 4, -1, 7, 8}));                   // 23
    }
}
