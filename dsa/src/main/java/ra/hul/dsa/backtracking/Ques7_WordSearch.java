package ra.hul.dsa.backtracking;

/**
 * Word Search - does a word exist in a grid via sequentially adjacent (no reuse) cells.
 *
 * Time: O(m * n * 4^L), Space: O(L) - recursion depth (board mutated then restored)
 */
public class Ques7_WordSearch {

    public static boolean exist(char[][] board, String word) {
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (dfs(board, word, r, c, 0)) return true;
            }
        }
        return false;
    }

    private static boolean dfs(char[][] board, String word, int r, int c, int index) {
        if (index == word.length()) return true;                 // matched all characters
        if (r < 0 || r >= board.length || c < 0 || c >= board[0].length) return false;
        if (board[r][c] != word.charAt(index)) return false;

        char saved = board[r][c];
        board[r][c] = '#';                                       // mark visited (sentinel)
        boolean found = dfs(board, word, r + 1, c, index + 1)
                     || dfs(board, word, r - 1, c, index + 1)
                     || dfs(board, word, r, c + 1, index + 1)
                     || dfs(board, word, r, c - 1, index + 1);
        board[r][c] = saved;                                     // restore (backtrack)
        return found;
    }

    static void main() {
        char[][] board = {
            {'A', 'B', 'C', 'E'},
            {'S', 'F', 'C', 'S'},
            {'A', 'D', 'E', 'E'}
        };
        System.out.println(exist(board, "ABCCED")); // true
        System.out.println(exist(board, "SEE"));    // true
        System.out.println(exist(board, "ABCB"));   // false
    }
}
