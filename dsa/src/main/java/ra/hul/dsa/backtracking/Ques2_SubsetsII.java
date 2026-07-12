package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subsets II - all subsets of an array that may contain duplicates (no duplicate subsets).
 *
 * Time: O(n * 2^n), Space: O(n) - recursion + path
 */
public class Ques2_SubsetsII {

    public static List<List<Integer>> subsetsWithDup(int[] nums) {
        Arrays.sort(nums);                      // duplicates become adjacent
        List<List<Integer>> res = new ArrayList<>();
        backtrack(0, nums, new ArrayList<>(), res);
        return res;
    }

    private static void backtrack(int start, int[] nums, List<Integer> cur, List<List<Integer>> res) {
        res.add(new ArrayList<>(cur));
        for (int i = start; i < nums.length; i++) {
            if (i > start && nums[i] == nums[i - 1]) continue;  // skip duplicate sibling at this level
            cur.add(nums[i]);                   // choose
            backtrack(i + 1, nums, cur, res);   // explore
            cur.remove(cur.size() - 1);         // un-choose
        }
    }

    static void main() {
        System.out.println(subsetsWithDup(new int[]{1, 2, 2})); // [[], [1], [1, 2], [1, 2, 2], [2], [2, 2]]
        System.out.println(subsetsWithDup(new int[]{0}));       // [[], [0]]
    }
}
