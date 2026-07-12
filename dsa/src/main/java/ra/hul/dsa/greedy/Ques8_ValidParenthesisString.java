package ra.hul.dsa.greedy;

/**
 * Valid Parenthesis String - is s valid where '*' can be '(', ')', or empty.
 *
 * Time: O(n), Space: O(1) - track the range [low, high] of possible open counts
 */
public class Ques8_ValidParenthesisString {

    public static boolean checkValidString(String s) {
        int low = 0;  // fewest possible open parens (treat * as ) or empty)
        int high = 0; // most possible open parens (treat * as ()
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                low++; high++;
            } else if (c == ')') {
                low--; high--;
            } else { // '*'
                low--; high++;
            }
            if (high < 0) return false; // too many closers even in the best case
            if (low < 0) low = 0;       // a count can't go negative; clamp
        }
        return low == 0; // some interpretation leaves zero unmatched opens
    }

    static void main() {
        System.out.println(checkValidString("()"));   // true
        System.out.println(checkValidString("(*)"));  // true
        System.out.println(checkValidString("(*))")); // true
        System.out.println(checkValidString(")("));   // false
        System.out.println(checkValidString("(("));   // false
    }
}
