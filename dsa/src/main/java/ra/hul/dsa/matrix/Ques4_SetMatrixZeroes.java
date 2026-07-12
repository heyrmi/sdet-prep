package ra.hul.dsa.matrix;

import java.util.Arrays;

/**
 * Set Matrix Zeroes - if an element is 0, set its entire row and column to 0, in place.
 *
 * Time: O(m * n), Space: O(1) - first row/col used as marker arrays
 */
public class Ques4_SetMatrixZeroes {

    public static void setZeroes(int[][] matrix) {
        int rows = matrix.length, cols = matrix[0].length;
        boolean firstColZero = false;

        // Pass 1: record which rows/cols must be zeroed, using row 0 and col 0 as flags.
        for (int r = 0; r < rows; r++) {
            if (matrix[r][0] == 0) firstColZero = true;   // col 0 tracked separately
            for (int c = 1; c < cols; c++) {
                if (matrix[r][c] == 0) {
                    matrix[r][0] = 0;
                    matrix[0][c] = 0;
                }
            }
        }

        // Pass 2: zero the interior based on the flags (skip row 0 and col 0 for now).
        for (int r = 1; r < rows; r++) {
            for (int c = 1; c < cols; c++) {
                if (matrix[r][0] == 0 || matrix[0][c] == 0) {
                    matrix[r][c] = 0;
                }
            }
        }

        // Pass 3: handle the first row and first column last.
        if (matrix[0][0] == 0) {
            for (int c = 0; c < cols; c++) matrix[0][c] = 0;
        }
        if (firstColZero) {
            for (int r = 0; r < rows; r++) matrix[r][0] = 0;
        }
    }

    static void main() {
        int[][] a = {{1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
        setZeroes(a);
        System.out.println(Arrays.deepToString(a)); // [[1, 0, 1], [0, 0, 0], [1, 0, 1]]

        int[][] b = {{0, 1, 2, 0}, {3, 4, 5, 2}, {1, 3, 1, 5}};
        setZeroes(b);
        System.out.println(Arrays.deepToString(b)); // [[0, 0, 0, 0], [0, 4, 5, 0], [0, 3, 1, 0]]
    }
}
