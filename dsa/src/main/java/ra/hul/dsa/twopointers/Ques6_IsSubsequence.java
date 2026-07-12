package ra.hul.dsa.twopointers;

/**
 * Is Subsequence - check whether s is a subsequence of t, one pointer per string.
 *
 * Time: O(|t|), Space: O(1)
 */
public class Ques6_IsSubsequence {

    public static boolean isSubsequence(String s, String t) {
        int i = 0, j = 0;
        while (i < s.length() && j < t.length()) {
            if (s.charAt(i) == t.charAt(j)) {
                i++; // matched this character of s
            }
            j++;     // always move forward through t
        }
        return i == s.length(); // matched all of s, in order
    }

    static void main() {
        System.out.println(isSubsequence("abc", "ahbgdc")); // true
        System.out.println(isSubsequence("axc", "ahbgdc")); // false
        System.out.println(isSubsequence("", "anything"));  // true
        System.out.println(isSubsequence("abc", "ab"));     // false
    }
}
