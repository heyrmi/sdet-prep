package ra.hul.dsa.backtracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Palindrome Partitioning - all partitionings of s where every substring is a palindrome.
 *
 * Time: O(n * 2^n), Space: O(n) - recursion depth
 */
public class Ques8_PalindromePartitioning {

    public static List<List<String>> partition(String s) {
        List<List<String>> res = new ArrayList<>();
        backtrack(s, 0, new ArrayList<>(), res);
        return res;
    }

    private static void backtrack(String s, int start, List<String> cur, List<List<String>> res) {
        if (start == s.length()) {
            res.add(new ArrayList<>(cur));          // whole string consumed — record a COPY
            return;
        }
        for (int end = start; end < s.length(); end++) {
            if (isPalindrome(s, start, end)) {      // only valid pieces (the prune)
                cur.add(s.substring(start, end + 1)); // choose (end + 1: exclusive bound)
                backtrack(s, end + 1, cur, res);      // explore
                cur.remove(cur.size() - 1);           // un-choose
            }
        }
    }

    private static boolean isPalindrome(String s, int lo, int hi) {
        while (lo < hi) {
            if (s.charAt(lo++) != s.charAt(hi--)) return false;
        }
        return true;
    }

    static void main() {
        System.out.println(partition("aab")); // [[a, a, b], [aa, b]]
        System.out.println(partition("a"));   // [[a]]
        System.out.println(partition("abc")); // [[a, b, c]]
    }
}
