package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Combination Sum - all unique combinations of candidates (unlimited reuse) that sum to target.
 *
 * Time: exponential in reaching sums, Space: O(target / min) - recursion depth
 */
public class Ques6_CombinationSum {

    public static List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> res = new ArrayList<>();
        backtrack(0, target, candidates, new ArrayList<>(), res);
        return res;
    }

    private static void backtrack(int start, int remaining, int[] candidates,
                                  List<Integer> cur, List<List<Integer>> res) {
        if (remaining == 0) {
            res.add(new ArrayList<>(cur));          // exact hit — record a COPY
            return;
        }
        if (remaining < 0) return;                  // prune: overshot (candidates are positive)
        for (int i = start; i < candidates.length; i++) {
            cur.add(candidates[i]);                 // choose
            backtrack(i, remaining - candidates[i], candidates, cur, res); // i => reuse allowed
            cur.remove(cur.size() - 1);             // un-choose
        }
    }

    static void main() {
        System.out.println(combinationSum(new int[]{2, 3, 6, 7}, 7)); // [[2, 2, 3], [7]]
        System.out.println(combinationSum(new int[]{2, 3, 5}, 8));    // [[2, 2, 2, 2], [2, 3, 3], [3, 5]]
        System.out.println(combinationSum(new int[]{2}, 1));          // []
    }
}
