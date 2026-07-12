package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Permutations - all permutations of an array of distinct integers.
 *
 * Time: O(n * n!), Space: O(n) - recursion + mask + path
 */
public class Ques3_Permutations {

    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        backtrack(nums, new boolean[nums.length], new ArrayList<>(), res);
        return res;
    }

    private static void backtrack(int[] nums, boolean[] used, List<Integer> cur, List<List<Integer>> res) {
        if (cur.size() == nums.length) {
            res.add(new ArrayList<>(cur));          // a full permutation — record a COPY
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;                  // already placed on this path
            used[i] = true; cur.add(nums[i]);       // choose
            backtrack(nums, used, cur, res);        // explore
            used[i] = false; cur.remove(cur.size() - 1); // un-choose (both)
        }
    }

    static void main() {
        System.out.println(permute(new int[]{1, 2, 3})); // [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]
        System.out.println(permute(new int[]{0, 1}));    // [[0, 1], [1, 0]]
    }
}
