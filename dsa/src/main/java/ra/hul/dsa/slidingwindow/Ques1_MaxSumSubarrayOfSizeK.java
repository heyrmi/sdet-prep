package ra.hul.dsa.slidingwindow;

/**
 * Maximum Sum Subarray of Size K - Find the max sum among all contiguous subarrays of size k.
 * (Easy - classic intro to sliding window)
 *
 * Time: O(n), Space: O(1)
 */
public class Ques1_MaxSumSubarrayOfSizeK {

    public static int maxSumSubarray(int[] nums, int k) {
        int windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += nums[i];
        }

        int maxSum = windowSum;
        for (int i = k; i < nums.length; i++) {
            windowSum += nums[i] - nums[i - k]; // slide: add right, remove left
            maxSum = Math.max(maxSum, windowSum);
        }
        return maxSum;
    }

    public static void main(String[] args) {
        System.out.println(maxSumSubarray(new int[]{2, 1, 5, 1, 3, 2}, 3)); // 9 (5+1+3)
        System.out.println(maxSumSubarray(new int[]{2, 3, 4, 1, 5}, 2));    // 7 (3+4)
    }
}
