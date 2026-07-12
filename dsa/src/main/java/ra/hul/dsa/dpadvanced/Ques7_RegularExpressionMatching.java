package ra.hul.dsa.dpadvanced;

/**
 * Regular Expression Matching - Full match of s against pattern p supporting '.' and '*'.
 *
 * Time: O(m*n), Space: O(m*n) - dp[i][j] = does s[0..i) match p[0..j)?
 */
public class Ques7_RegularExpressionMatching {

    public static boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;                          // empty matches empty

        // Base row: empty s vs. patterns that can collapse to empty (a*, a*b*, .* ...).
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 2];          // '*' takes zero copies of p[j-2]
            }
        }

        for (int i = 1; i <= m; i++) {
            char sc = s.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    char prev = p.charAt(j - 2);
                    boolean zero = dp[i][j - 2];                       // drop "prev*"
                    boolean more = (prev == '.' || prev == sc) && dp[i - 1][j]; // eat one s char
                    dp[i][j] = zero || more;
                } else {
                    dp[i][j] = (pc == '.' || pc == sc) && dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }

    static void main() {
        System.out.println(isMatch("aa", "a"));                    // false
        System.out.println(isMatch("aa", "a*"));                   // true
        System.out.println(isMatch("ab", ".*"));                   // true
        System.out.println(isMatch("aab", "c*a*b"));               // true
        System.out.println(isMatch("mississippi", "mis*is*p*."));  // false
    }
}
