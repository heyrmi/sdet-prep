package ra.hul.dsa.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ques7_3Sum {

    public static List<List<Integer>> threeSum(int[] nums) {
        // Sort this to convert it to 2 pointers problem
        Arrays.sort(nums);

        List<List<Integer>> result = new ArrayList<>();

        for (int i = 0; i < nums.length; i++) {
            // if the same number comes up again skip it to avoid duplicacy
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            // if smallest candidate is positive no triplet exists
            if (nums[i] > 0) break;

            // fix one number then run 2 pointers on rest 2 numbers
            int left = i + 1;
            int right = nums.length - 1;

            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];

                if (sum == 0) {
                    // found triplet
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    // skip duplicates
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    // continue to check further
                    left++;
                    right--;
                } else if (sum < 0) {
                    left++; // increase from left and add higher values
                } else {
                    right--; // decrease from right and add less values
                }
            }
        }
        return result;
    }

    static void main() {
        System.out.println(threeSum(new int[]{0, 1, 1}));               // []
        System.out.println(threeSum(new int[]{0, 0, 0}));               // [[0, 0, 0]]
        System.out.println(threeSum(new int[]{-2, 0, 1, 1, 2}));        // [[-2, 0, 2], [-2, 1, 1]]
        System.out.println(threeSum(new int[]{-1, 0, 1, 2, -1, -4}));   // [[-1, -1, 2], [-1, 0, 1]]
    }
}
