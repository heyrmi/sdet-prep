package ra.hul.dsa.twopointers;

import java.util.Arrays;

/**
 * Two Sum II (Sorted) - find two numbers in a sorted array that add up to target; return 1-based indices.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques2_TwoSumIISorted {

    public static int[] twoSum(int[] numbers, int target) {
        int left = 0, right = numbers.length - 1;
        while (left < right) {
            long sum = (long) numbers[left] + numbers[right];
            if (sum == target) {
                return new int[]{left + 1, right + 1}; // 1-based indices
            } else if (sum < target) {
                left++;   // need a bigger sum
            } else {
                right--;  // need a smaller sum
            }
        }
        return new int[]{}; // unreachable given the problem guarantee
    }

    static void main() {
        System.out.println(Arrays.toString(twoSum(new int[]{2, 7, 11, 15}, 9))); // [1, 2]
        System.out.println(Arrays.toString(twoSum(new int[]{2, 3, 4}, 6)));      // [1, 3]
        System.out.println(Arrays.toString(twoSum(new int[]{-1, 0}, -1)));       // [1, 2]
    }
}
