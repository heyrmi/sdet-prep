package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Subsets - Generate all possible subsets (power set) of an integer array.
 * LeetCode #78 (Medium, but a classic intro to backtracking)
 *
 * Time: O(n * 2^n), Space: O(n) recursion depth
 */
public class Ques1_Subsets {

    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrack(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
        result.add(new ArrayList<>(current));
        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]);
            backtrack(nums, i + 1, current, result);
            current.remove(current.size() - 1); // undo choice
        }
    }

    public static void main(String[] args) {
        System.out.println(subsets(new int[]{1, 2, 3}));
        // [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]]
    }
}
