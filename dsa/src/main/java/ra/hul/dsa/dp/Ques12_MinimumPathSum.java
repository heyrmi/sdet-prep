package ra.hul.dsa.dp;

/**
 * Minimum Path Sum - Cheapest right/down path from top-left to bottom-right of a cost grid.
 *
 * Time: O(m*n), Space: O(m*n) - dp[i][j] = grid[i][j] + min(dp[i-1][j], dp[i][j-1]).
 */
public class Ques12_MinimumPathSum {

    public static int minPathSum(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[][] dp = new int[m][n];
        dp[0][0] = grid[0][0];
        for (int j = 1; j < n; j++) dp[0][j] = dp[0][j - 1] + grid[0][j]; // top row: one way in
        for (int i = 1; i < m; i++) dp[i][0] = dp[i - 1][0] + grid[i][0]; // left col: one way in
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = grid[i][j] + Math.min(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[m - 1][n - 1];
    }

    static void main() {
        System.out.println(minPathSum(new int[][]{{1, 3, 1}, {1, 5, 1}, {4, 2, 1}})); // 7
        System.out.println(minPathSum(new int[][]{{1, 2, 3}, {4, 5, 6}}));            // 12
        System.out.println(minPathSum(new int[][]{{5}}));                             // 5
    }
}
