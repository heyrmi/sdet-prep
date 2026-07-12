package ra.hul.dsa.dp;

/**
 * Edit Distance - Minimum insert/delete/replace operations to turn word1 into word2.
 *
 * Time: O(m*n), Space: O(m*n) - dp[i][j] = min ops for word1[0..i) -> word2[0..j).
 */
public class Ques14_EditDistance {

    public static int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i; // delete i chars to reach empty
        for (int j = 0; j <= n; j++) dp[0][j] = j; // insert j chars from empty
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // characters agree: no extra op
                } else {
                    int delete = dp[i - 1][j];
                    int insert = dp[i][j - 1];
                    int replace = dp[i - 1][j - 1];
                    dp[i][j] = 1 + Math.min(delete, Math.min(insert, replace));
                }
            }
        }
        return dp[m][n];
    }

    static void main() {
        System.out.println(minDistance("horse", "ros"));            // 3
        System.out.println(minDistance("intention", "execution"));  // 5
        System.out.println(minDistance("", "abc"));                 // 3
        System.out.println(minDistance("abc", "abc"));              // 0
    }
}
