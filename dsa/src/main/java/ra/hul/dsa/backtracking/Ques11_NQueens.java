package ra.hul.dsa.backtracking;

/**
 * N-Queens - count distinct placements of n non-attacking queens on an n x n board.
 *
 * Time: ~O(n!) with pruning, Space: O(n) - recursion + conflict arrays
 */
public class Ques11_NQueens {

    public static int totalNQueens(int n) {
        boolean[] cols = new boolean[n];
        boolean[] diag1 = new boolean[2 * n - 1];   // row + col, range 0 .. 2n-2
        boolean[] diag2 = new boolean[2 * n - 1];   // row - col + (n-1), range 0 .. 2n-2
        return backtrack(0, n, cols, diag1, diag2);
    }

    private static int backtrack(int row, int n, boolean[] cols, boolean[] diag1, boolean[] diag2) {
        if (row == n) return 1;                     // placed a queen in every row — one solution
        int count = 0;
        for (int col = 0; col < n; col++) {
            int d1 = row + col;
            int d2 = row - col + (n - 1);
            if (cols[col] || diag1[d1] || diag2[d2]) continue;  // prune conflict
            cols[col] = diag1[d1] = diag2[d2] = true;           // choose
            count += backtrack(row + 1, n, cols, diag1, diag2); // explore next row
            cols[col] = diag1[d1] = diag2[d2] = false;          // un-choose
        }
        return count;
    }

    static void main() {
        System.out.println(totalNQueens(1)); // 1
        System.out.println(totalNQueens(4)); // 2
        System.out.println(totalNQueens(8)); // 92
    }
}
