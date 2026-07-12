package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Letter Combinations of a Phone Number - all letter strings a digit string could spell on a keypad.
 *
 * Time: O(4^d * d), Space: O(d) - recursion depth
 */
public class Ques10_LetterCombinationsOfAPhoneNumber {

    private static final String[] MAP = {
        "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };

    public static List<String> letterCombinations(String digits) {
        List<String> res = new ArrayList<>();
        if (digits == null || digits.isEmpty()) return res;  // empty input -> empty list
        backtrack(digits, 0, new StringBuilder(), res);
        return res;
    }

    private static void backtrack(String digits, int index, StringBuilder sb, List<String> res) {
        if (index == digits.length()) {
            res.add(sb.toString());
            return;
        }
        String letters = MAP[digits.charAt(index) - '0'];
        for (int i = 0; i < letters.length(); i++) {
            sb.append(letters.charAt(i));               // choose
            backtrack(digits, index + 1, sb, res);      // explore next digit
            sb.deleteCharAt(sb.length() - 1);           // un-choose
        }
    }

    static void main() {
        System.out.println(letterCombinations("23")); // [ad, ae, af, bd, be, bf, cd, ce, cf]
        System.out.println(letterCombinations(""));   // []
        System.out.println(letterCombinations("2"));  // [a, b, c]
    }
}
