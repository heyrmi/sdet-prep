package ra.hul.dsa.dp;

/**
 * Maximal Square - Area of the largest all-1s square in a binary char matrix.
 *
 * Time: O(m*n), Space: O(m*n) - dp[i][j] = 1 + min(up, left, diagonal); answer = maxSide^2.
 */
public class Ques15_MaximalSquare {

    public static int maximalSquare(char[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        int[][] dp = new int[m + 1][n + 1]; // sentinel row/col of 0s
        int maxSide = 0;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (matrix[i - 1][j - 1] == '1') {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j],
                                   Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                    maxSide = Math.max(maxSide, dp[i][j]);
                }
            }
        }
        return maxSide * maxSide;
    }

    static void main() {
        System.out.println(maximalSquare(new char[][]{
                {'1', '0', '1', '0', '0'},
                {'1', '0', '1', '1', '1'},
                {'1', '1', '1', '1', '1'},
                {'1', '0', '0', '1', '0'}})); // 4
        System.out.println(maximalSquare(new char[][]{{'0', '1'}, {'1', '0'}})); // 1
        System.out.println(maximalSquare(new char[][]{{'0'}}));                  // 0
    }
}
