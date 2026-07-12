package ra.hul.dsa.dpadvanced;

/**
 * Distinct Subsequences - Number of distinct subsequences of s that equal t.
 *
 * Time: O(m*n), Space: O(m*n) - dp[i][j] = ways t[0..j) appears as a subsequence of s[0..i); table in long.
 */
public class Ques8_DistinctSubsequences {

    public static int numDistinct(String s, String t) {
        int m = s.length(), n = t.length();
        long[][] dp = new long[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = 1;   // one way to form empty t
        // dp[0][j] = 0 for j > 0 by default (empty s can't form non-empty t)

        for (int i = 1; i <= m; i++) {
            char sc = s.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                dp[i][j] = dp[i - 1][j];                          // skip s[i-1]
                if (sc == t.charAt(j - 1)) {
                    dp[i][j] += dp[i - 1][j - 1];                 // also use s[i-1] to match
                }
            }
        }
        return (int) dp[m][n];   // problem guarantees the result fits in a 32-bit int
    }

    static void main() {
        System.out.println(numDistinct("rabbbit", "rabbit")); // 3
        System.out.println(numDistinct("babgbag", "bag"));    // 5
        System.out.println(numDistinct("abc", ""));           // 1
        System.out.println(numDistinct("", "a"));             // 0
        System.out.println(numDistinct("aaa", "aa"));         // 3
    }
}
