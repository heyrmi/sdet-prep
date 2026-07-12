package ra.hul.dsa.dp;

/**
 * Unique Paths - Distinct right/down paths through an m x n grid.
 *
 * Time: O(m*n), Space: O(m*n) - dp[i][j] = dp[i-1][j] + dp[i][j-1].
 */
public class Ques10_UniquePaths {

    public static int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];
        for (int j = 0; j < n; j++) dp[0][j] = 1; // top row: one straight-line way (go right)
        for (int i = 0; i < m; i++) dp[i][0] = 1; // left col: one straight-line way (go down)
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
            }
        }
        return dp[m - 1][n - 1];
    }

    static void main() {
        System.out.println(uniquePaths(3, 7));  // 28
        System.out.println(uniquePaths(3, 2));  // 3
        System.out.println(uniquePaths(1, 1));  // 1
        System.out.println(uniquePaths(1, 10)); // 1
    }
}
