package ra.hul.dsa.dpadvanced;

/**
 * Longest Palindromic Substring - Longest contiguous palindromic substring of s.
 *
 * Time: O(n^2), Space: O(1) - expand around each of the 2n-1 centers; track the best [start, end].
 */
public class Ques3_LongestPalindromicSubstring {

    public static String longestPalindrome(String s) {
        if (s == null || s.isEmpty()) return "";
        int start = 0, end = 0;                 // best palindrome span (inclusive)
        for (int i = 0; i < s.length(); i++) {
            int[] odd = expand(s, i, i);        // odd-length center
            if (odd[1] - odd[0] > end - start) { start = odd[0]; end = odd[1]; }
            int[] even = expand(s, i, i + 1);   // even-length center
            if (even[1] - even[0] > end - start) { start = even[0]; end = even[1]; }
        }
        return s.substring(start, end + 1);
    }

    // Expand while the two sides match; return the inclusive [lo, hi] of the palindrome found.
    static int[] expand(String s, int lo, int hi) {
        while (lo >= 0 && hi < s.length() && s.charAt(lo) == s.charAt(hi)) {
            lo--;
            hi++;
        }
        // lo/hi overshoot by one when the loop stops; the palindrome is [lo+1, hi-1].
        return new int[]{lo + 1, hi - 1};
    }

    static void main() {
        System.out.println(longestPalindrome("babad")); // bab
        System.out.println(longestPalindrome("cbbd"));  // bb
        System.out.println(longestPalindrome("a"));     // a
        System.out.println(longestPalindrome("ac"));    // a
        System.out.println(longestPalindrome(""));      // (empty)
    }
}
