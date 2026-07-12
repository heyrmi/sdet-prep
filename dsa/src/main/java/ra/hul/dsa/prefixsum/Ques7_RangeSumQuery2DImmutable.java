package ra.hul.dsa.prefixsum;

/**
 * Range Sum Query 2D Immutable - answer many rectangle-sum queries via a 2D prefix and inclusion-exclusion.
 *
 * Time: O(m*n) build, O(1) per query. Space: O(m*n)
 */
public class Ques7_RangeSumQuery2DImmutable {

    static class NumMatrix {
        // P[i][j] = sum of the rectangle (0,0)..(i-1,j-1); zero sentinel row/column.
        private final long[][] P;

        NumMatrix(int[][] matrix) {
            int rows = matrix.length;
            int cols = matrix[0].length;
            P = new long[rows + 1][cols + 1];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    P[i + 1][j + 1] = matrix[i][j] + P[i][j + 1] + P[i + 1][j] - P[i][j];
                }
            }
        }

        int sumRegion(int row1, int col1, int row2, int col2) {
            long total = P[row2 + 1][col2 + 1] // whole block down to bottom-right
                       - P[row1][col2 + 1]     // subtract the strip above
                       - P[row2 + 1][col1]     // subtract the strip to the left
                       + P[row1][col1];        // add back the overlap removed twice
            return (int) total;
        }
    }

    static void main() {
        int[][] matrix = {
            {3, 0, 1, 4, 2},
            {5, 6, 3, 2, 1},
            {1, 2, 0, 1, 5},
            {4, 1, 0, 1, 7},
            {1, 0, 3, 0, 5}
        };
        NumMatrix nm = new NumMatrix(matrix);
        System.out.println(nm.sumRegion(2, 1, 4, 3)); // 8
        System.out.println(nm.sumRegion(1, 1, 2, 2)); // 11
        System.out.println(nm.sumRegion(1, 2, 2, 4)); // 12
    }
}
