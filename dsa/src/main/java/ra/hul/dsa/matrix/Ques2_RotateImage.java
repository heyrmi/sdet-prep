package ra.hul.dsa.matrix;

import java.util.Arrays;

/**
 * Rotate Image - rotate an n x n matrix 90 degrees clockwise in place.
 *
 * Time: O(n^2), Space: O(1) - transpose then reverse each row
 */
public class Ques2_RotateImage {

    public static void rotate(int[][] matrix) {
        int n = matrix.length;

        // 1) Transpose: swap across the main diagonal (upper triangle only).
        for (int r = 0; r < n; r++) {
            for (int c = r + 1; c < n; c++) {
                int tmp = matrix[r][c];
                matrix[r][c] = matrix[c][r];
                matrix[c][r] = tmp;
            }
        }

        // 2) Reverse each row in place.
        for (int r = 0; r < n; r++) {
            for (int lo = 0, hi = n - 1; lo < hi; lo++, hi--) {
                int tmp = matrix[r][lo];
                matrix[r][lo] = matrix[r][hi];
                matrix[r][hi] = tmp;
            }
        }
    }

    static void main() {
        int[][] a = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        rotate(a);
        System.out.println(Arrays.deepToString(a)); // [[7, 4, 1], [8, 5, 2], [9, 6, 3]]

        int[][] b = {{5, 1, 9, 11}, {2, 4, 8, 10}, {13, 3, 6, 7}, {15, 14, 12, 16}};
        rotate(b);
        System.out.println(Arrays.deepToString(b)); // [[15, 13, 2, 5], [14, 3, 4, 1], [12, 6, 8, 9], [16, 7, 10, 11]]
    }
}
