package ra.hul.dsa.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pacific Atlantic Water Flow - cells that drain to both oceans via reverse multi-source DFS.
 * LeetCode #417 (Medium)
 *
 * Time: O(m*n), Space: O(m*n)
 */
public class Ques9_PacificAtlanticWaterFlow {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    public static List<int[]> pacificAtlantic(int[][] heights) {
        List<int[]> result = new ArrayList<>();
        if (heights == null || heights.length == 0 || heights[0].length == 0) return result;

        int m = heights.length, n = heights[0].length;
        boolean[][] pacific = new boolean[m][n];
        boolean[][] atlantic = new boolean[m][n];

        // Pacific: top row + left column.  Atlantic: bottom row + right column.
        for (int c = 0; c < n; c++) {
            dfs(heights, 0, c, pacific);
            dfs(heights, m - 1, c, atlantic);
        }
        for (int r = 0; r < m; r++) {
            dfs(heights, r, 0, pacific);
            dfs(heights, r, n - 1, atlantic);
        }

        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (pacific[r][c] && atlantic[r][c]) result.add(new int[]{r, c});
            }
        }
        return result;
    }

    // Flood from (r,c) to neighbors of EQUAL OR GREATER height (reverse of downhill flow).
    private static void dfs(int[][] heights, int r, int c, boolean[][] reached) {
        reached[r][c] = true;
        int m = heights.length, n = heights[0].length;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;   // bounds first
            if (reached[nr][nc]) continue;
            if (heights[nr][nc] < heights[r][c]) continue;          // can't climb down then back
            dfs(heights, nr, nc, reached);
        }
    }

    static void main() {
        int[][] heights = {
            {1, 2, 2, 3, 5},
            {3, 2, 3, 4, 4},
            {2, 4, 5, 3, 1},
            {6, 7, 1, 4, 5},
            {5, 1, 1, 2, 4}
        };
        int[][] cells = pacificAtlantic(heights).toArray(new int[0][]);
        // [[0, 4], [1, 3], [1, 4], [2, 2], [3, 0], [3, 1], [4, 0]]
        System.out.println(Arrays.deepToString(cells));
    }
}
