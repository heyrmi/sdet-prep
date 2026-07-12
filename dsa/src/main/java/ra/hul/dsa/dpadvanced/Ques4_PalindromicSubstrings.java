package ra.hul.dsa.dpadvanced;

/**
 * Palindromic Substrings - Count of palindromic substrings in s (by position).
 *
 * Time: O(n^2), Space: O(1) - each successful expansion step is one more palindromic substring.
 */
public class Ques4_PalindromicSubstrings {

    public static int countSubstrings(String s) {
        if (s == null || s.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            count += expand(s, i, i);       // odd-length centers
            count += expand(s, i, i + 1);   // even-length centers
        }
        return count;
    }

    // Number of palindromes centered at (lo, hi).
    static int expand(String s, int lo, int hi) {
        int found = 0;
        while (lo >= 0 && hi < s.length() && s.charAt(lo) == s.charAt(hi)) {
            found++;
            lo--;
            hi++;
        }
        return found;
    }

    static void main() {
        System.out.println(countSubstrings("abc")); // 3
        System.out.println(countSubstrings("aaa")); // 6
        System.out.println(countSubstrings("a"));   // 1
        System.out.println(countSubstrings(""));    // 0
    }
}
