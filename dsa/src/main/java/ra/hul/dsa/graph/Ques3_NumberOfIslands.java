package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Number of Islands - count connected groups of '1' land cells (4-directional) in a grid.
 * LeetCode #200 (Medium)
 *
 * Time: O(m*n), Space: O(min(m,n)) for the BFS queue
 */
public class Ques3_NumberOfIslands {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    public static int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) return 0;
        int rows = grid.length, cols = grid[0].length, count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '1') {
                    count++;
                    flood(grid, r, c);
                }
            }
        }
        return count;
    }

    /** BFS flood fill: sink every land cell connected to (sr, sc). */
    private static void flood(char[][] grid, int sr, int sc) {
        int rows = grid.length, cols = grid[0].length;
        Deque<int[]> q = new ArrayDeque<>();
        grid[sr][sc] = '0';                 // sink on enqueue
        q.add(new int[]{sr, sc});
        while (!q.isEmpty()) {
            int[] cell = q.poll();
            for (int[] d : DIRS) {
                int nr = cell[0] + d[0], nc = cell[1] + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue; // bounds first
                if (grid[nr][nc] == '1') {
                    grid[nr][nc] = '0';
                    q.add(new int[]{nr, nc});
                }
            }
        }
    }

    static void main() {
        char[][] grid1 = {
            {'1', '1', '0', '0', '0'},
            {'1', '1', '0', '0', '0'},
            {'0', '0', '1', '0', '0'},
            {'0', '0', '0', '1', '1'}
        };
        System.out.println(numIslands(grid1));   // 3

        char[][] grid2 = {
            {'1', '1', '1'},
            {'0', '1', '0'},
            {'1', '1', '1'}
        };
        System.out.println(numIslands(grid2));   // 1
    }
}
