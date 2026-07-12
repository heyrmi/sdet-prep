package ra.hul.dsa.dp;

/**
 * Unique Paths II - Distinct right/down paths through a grid with obstacles (1 = blocked).
 *
 * Time: O(m*n), Space: O(m*n) - same recurrence as Unique Paths, obstacle cells pinned to 0 paths.
 */
public class Ques11_UniquePathsII {

    public static int uniquePathsWithObstacles(int[][] obstacleGrid) {
        int m = obstacleGrid.length, n = obstacleGrid[0].length;
        int[][] dp = new int[m][n]; // defaults to 0 = "unreachable"
        dp[0][0] = obstacleGrid[0][0] == 1 ? 0 : 1;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (obstacleGrid[i][j] == 1) {
                    dp[i][j] = 0; // blocked: no path stands here
                    continue;
                }
                if (i == 0 && j == 0) continue; // already seeded
                int fromUp = i > 0 ? dp[i - 1][j] : 0;
                int fromLeft = j > 0 ? dp[i][j - 1] : 0;
                dp[i][j] = fromUp + fromLeft;
            }
        }
        return dp[m - 1][n - 1];
    }

    static void main() {
        System.out.println(uniquePathsWithObstacles(new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 0, 0}})); // 2
        System.out.println(uniquePathsWithObstacles(new int[][]{{0, 1}, {0, 0}}));                   // 1
        System.out.println(uniquePathsWithObstacles(new int[][]{{1}}));                              // 0
        System.out.println(uniquePathsWithObstacles(new int[][]{{0, 0}, {1, 1}, {0, 0}}));           // 0
    }
}
