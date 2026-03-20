package ra.hul.dsa.matrix;

import java.util.Arrays;

/**
 * Transpose Matrix - Flip a matrix over its diagonal (rows become columns).
 * LeetCode #867 (Easy)
 *
 * Time: O(m*n), Space: O(m*n) for result
 */
public class Ques1_TransposeMatrix {

    public static int[][] transpose(int[][] matrix) {
        int rows = matrix.length, cols = matrix[0].length;
        int[][] result = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        int[][] transposed = transpose(matrix);
        // [[1,4,7],[2,5,8],[3,6,9]]
        for (int[] row : transposed) System.out.println(Arrays.toString(row));

        System.out.println();

        int[][] rect = {{1, 2, 3}, {4, 5, 6}};
        int[][] transposedRect = transpose(rect);
        // [[1,4],[2,5],[3,6]]
        for (int[] row : transposedRect) System.out.println(Arrays.toString(row));
    }
}
