package ra.hul.dsa.trie;

import java.util.ArrayList;
import java.util.List;

/**
 * Word Search II - find all dictionary words on a grid using a trie + DFS with pruning.
 * LeetCode #212 (Hard)
 *
 * Time: O(m*n*4*3^(L-1)) worst case (trie pruning cuts it hard in practice), Space: O(total word chars)
 */
public class Ques4_WordSearchIi {

    static class BoardNode {
        BoardNode[] children = new BoardNode[26];
        String word = null;
    }

    static List<String> findWords(char[][] board, String[] words) {
        BoardNode root = new BoardNode();
        for (String w : words) {
            insert(root, w);
        }
        List<String> results = new ArrayList<>();
        if (board == null || board.length == 0) return results;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                dfs(board, r, c, root, results);
            }
        }
        return results;
    }

    private static void insert(BoardNode root, String word) {
        BoardNode node = root;
        for (int i = 0; i < word.length(); i++) {
            int idx = word.charAt(i) - 'a';
            if (node.children[idx] == null) {
                node.children[idx] = new BoardNode();
            }
            node = node.children[idx];
        }
        node.word = word;
    }

    private static void dfs(char[][] board, int r, int c, BoardNode node, List<String> results) {
        char ch = board[r][c];
        if (ch == '#') return;                 // already on the current path
        BoardNode next = node.children[ch - 'a'];
        if (next == null) return;              // PRUNE: no dictionary word continues here

        if (next.word != null) {
            results.add(next.word);
            next.word = null;                  // de-dup: don't report the same word twice
        }

        board[r][c] = '#';                     // mark visited
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < board.length && nc >= 0 && nc < board[0].length) {
                dfs(board, nr, nc, next, results);
            }
        }
        board[r][c] = ch;                      // restore (backtrack)
    }

    static void main() {
        char[][] board = {
            {'o', 'a', 'a', 'n'},
            {'e', 't', 'a', 'e'},
            {'i', 'h', 'k', 'r'},
            {'i', 'f', 'l', 'v'}
        };
        String[] words = {"oath", "pea", "eat", "rain"};
        System.out.println(findWords(board, words)); // [oath, eat]
    }
}
