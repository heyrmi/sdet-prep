package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate Parentheses - all combinations of n pairs of well-formed parentheses.
 *
 * Time: O(4^n / sqrt(n)) (Catalan), Space: O(n) - recursion depth
 */
public class Ques9_GenerateParentheses {

    public static List<String> generateParenthesis(int n) {
        List<String> res = new ArrayList<>();
        backtrack(0, 0, n, new StringBuilder(), res);
        return res;
    }

    private static void backtrack(int open, int close, int n, StringBuilder sb, List<String> res) {
        if (sb.length() == 2 * n) {
            res.add(sb.toString());                 // complete valid string — immutable snapshot
            return;
        }
        if (open < n) {                             // can still open
            sb.append('(');
            backtrack(open + 1, close, n, sb, res);
            sb.deleteCharAt(sb.length() - 1);       // undo
        }
        if (close < open) {                         // can close only if an open is unmatched
            sb.append(')');
            backtrack(open, close + 1, n, sb, res);
            sb.deleteCharAt(sb.length() - 1);       // undo
        }
    }

    static void main() {
        System.out.println(generateParenthesis(1)); // [()]
        System.out.println(generateParenthesis(3)); // [((())), (()()), (())(), ()(()), ()()()]
    }
}
