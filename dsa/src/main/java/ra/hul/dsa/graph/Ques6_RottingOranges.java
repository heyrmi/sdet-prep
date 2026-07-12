package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rotting Oranges - minutes until all fresh oranges rot via multi-source BFS (level = minute).
 * LeetCode #994 (Medium)
 *
 * Time: O(m*n), Space: O(m*n)
 */
public class Ques6_RottingOranges {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    public static int orangesRotting(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        Deque<int[]> q = new ArrayDeque<>();
        int fresh = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) q.add(new int[]{r, c});  // all rotten = sources
                else if (grid[r][c] == 1) fresh++;
            }
        }
        if (fresh == 0) return 0;                              // nothing to rot

        int minutes = 0;
        while (!q.isEmpty() && fresh > 0) {
            int sz = q.size();                                // one minute = one whole level
            for (int i = 0; i < sz; i++) {
                int[] cell = q.poll();
                for (int[] d : DIRS) {
                    int nr = cell[0] + d[0], nc = cell[1] + d[1];
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue; // bounds first
                    if (grid[nr][nc] == 1) {
                        grid[nr][nc] = 2;
                        fresh--;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
            minutes++;
        }
        return fresh == 0 ? minutes : -1;
    }

    static void main() {
        System.out.println(orangesRotting(new int[][]{{2, 1, 1}, {1, 1, 0}, {0, 1, 1}}));   // 4
        System.out.println(orangesRotting(new int[][]{{2, 1, 1}, {0, 1, 1}, {1, 0, 1}}));   // -1
        System.out.println(orangesRotting(new int[][]{{0, 2}}));                            // 0
    }
}
