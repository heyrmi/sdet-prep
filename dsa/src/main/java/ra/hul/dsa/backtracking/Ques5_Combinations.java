package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Combinations - all combinations of k numbers chosen from the range [1, n].
 *
 * Time: O(k * C(n, k)), Space: O(k) - recursion depth
 */
public class Ques5_Combinations {

    public static List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> res = new ArrayList<>();
        backtrack(1, n, k, new ArrayList<>(), res);
        return res;
    }

    private static void backtrack(int start, int n, int k, List<Integer> cur, List<List<Integer>> res) {
        if (cur.size() == k) {
            res.add(new ArrayList<>(cur));
            return;
        }
        // Prune: need (k - cur.size()) more; only values up to n - need + 1 can start a full combo.
        int need = k - cur.size();
        for (int i = start; i <= n - need + 1; i++) {
            cur.add(i);                             // choose
            backtrack(i + 1, n, k, cur, res);       // explore
            cur.remove(cur.size() - 1);             // un-choose
        }
    }

    static void main() {
        System.out.println(combine(4, 2)); // [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]
        System.out.println(combine(1, 1)); // [[1]]
    }
}
