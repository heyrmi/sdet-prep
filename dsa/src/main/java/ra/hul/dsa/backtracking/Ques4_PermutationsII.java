package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Permutations II - all unique permutations of an array that may contain duplicates.
 *
 * Time: O(n * n!), Space: O(n) - recursion + mask + path
 */
public class Ques4_PermutationsII {

    public static List<List<Integer>> permuteUnique(int[] nums) {
        Arrays.sort(nums);                          // equal values adjacent
        List<List<Integer>> res = new ArrayList<>();
        backtrack(nums, new boolean[nums.length], new ArrayList<>(), res);
        return res;
    }

    private static void backtrack(int[] nums, boolean[] used, List<Integer> cur, List<List<Integer>> res) {
        if (cur.size() == nums.length) {
            res.add(new ArrayList<>(cur));
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;
            // Skip a duplicate value unless its left twin is already placed (forces left-to-right use).
            if (i > 0 && nums[i] == nums[i - 1] && !used[i - 1]) continue;
            used[i] = true; cur.add(nums[i]);       // choose
            backtrack(nums, used, cur, res);        // explore
            used[i] = false; cur.remove(cur.size() - 1); // un-choose
        }
    }

    static void main() {
        System.out.println(permuteUnique(new int[]{1, 1, 2})); // [[1, 1, 2], [1, 2, 1], [2, 1, 1]]
        System.out.println(permuteUnique(new int[]{1, 2, 3})); // [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]
    }
}
