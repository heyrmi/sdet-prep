package ra.hul.dsa.matrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Spiral Matrix - return all elements of an m x n matrix in clockwise spiral order.
 *
 * Time: O(m * n), Space: O(1) - four moving walls, output aside
 */
public class Ques3_SpiralMatrix {

    public static List<Integer> spiralOrder(int[][] matrix) {
        List<Integer> out = new ArrayList<>();
        int top = 0, bottom = matrix.length - 1;
        int left = 0, right = matrix[0].length - 1;

        while (top <= bottom && left <= right) {
            for (int c = left; c <= right; c++) out.add(matrix[top][c]);
            top++;

            for (int r = top; r <= bottom; r++) out.add(matrix[r][right]);
            right--;

            if (top <= bottom) {                 // re-check: row may already be consumed
                for (int c = right; c >= left; c--) out.add(matrix[bottom][c]);
                bottom--;
            }

            if (left <= right) {                 // re-check: col may already be consumed
                for (int r = bottom; r >= top; r--) out.add(matrix[r][left]);
                left++;
            }
        }
        return out;
    }

    static void main() {
        System.out.println(spiralOrder(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));
        // [1, 2, 3, 6, 9, 8, 7, 4, 5]
        System.out.println(spiralOrder(new int[][]{{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}}));
        // [1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7]
    }
}
