package ra.hul.dsa.arrays;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Two Sum - Given an array of integers and a target, return indices of two numbers that add up to target.
 * LeetCode #1 (Easy)
 *
 * Time: O(n), Space: O(n) - using HashMap
 */
public class Ques1_TwoSum {

    public static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[]{map.get(complement), i};
            }
            map.put(nums[i], i);
        }
        return new int[]{};
    }

    static void main() {
        System.out.println(Arrays.toString(twoSum(new int[]{2, 7, 11, 15}, 9)));   // [0, 1]
        System.out.println(Arrays.toString(twoSum(new int[]{3, 2, 4}, 6)));        // [1, 2]
        System.out.println(Arrays.toString(twoSum(new int[]{3, 3}, 6)));           // [0, 1]
    }
}
