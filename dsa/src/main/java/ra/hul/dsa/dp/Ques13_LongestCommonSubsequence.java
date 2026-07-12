package ra.hul.dsa.dp;

/**
 * Longest Common Subsequence - Length of the LCS of two strings.
 *
 * Time: O(m*n), Space: O(m*n) - match -> diagonal+1, mismatch -> best of up/left.
 */
public class Ques13_LongestCommonSubsequence {

    public static int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1]; // sentinel empty-prefix row/col default to 0
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;                 // match: extend the diagonal
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]); // skip one side
                }
            }
        }
        return dp[m][n];
    }

    static void main() {
        System.out.println(longestCommonSubsequence("abcde", "ace")); // 3
        System.out.println(longestCommonSubsequence("abc", "abc"));   // 3
        System.out.println(longestCommonSubsequence("abc", "def"));   // 0
        System.out.println(longestCommonSubsequence("", "abc"));      // 0
    }
}
