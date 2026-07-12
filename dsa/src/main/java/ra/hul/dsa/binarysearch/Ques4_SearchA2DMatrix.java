package ra.hul.dsa.binarysearch;

/**
 * Search a 2D Matrix - binary-search the flattened row-major index space.
 * LeetCode #74 (Medium)
 *
 * Time: O(log(m*n)), Space: O(1)
 */
public class Ques4_SearchA2DMatrix {

    public static boolean searchMatrix(int[][] matrix, int target) {
        if (matrix.length == 0 || matrix[0].length == 0) return false;
        int m = matrix.length, n = matrix[0].length;
        int lo = 0, hi = m * n - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int v = matrix[mid / n][mid % n];     // n cells per row
            if (v == target) return true;
            else if (v < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return false;
    }

    static void main() {
        int[][] matrix = {
                {1, 3, 5, 7},
                {10, 11, 16, 20},
                {23, 30, 34, 60}
        };
        System.out.println(searchMatrix(matrix, 3));  // true
        System.out.println(searchMatrix(matrix, 13)); // false
    }
}
