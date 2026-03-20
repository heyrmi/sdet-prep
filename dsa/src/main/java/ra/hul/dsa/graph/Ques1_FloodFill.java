package ra.hul.dsa.graph;

import java.util.Arrays;

/**
 * Flood Fill - Starting from pixel (sr, sc), change all connected same-color pixels to newColor.
 * LeetCode #733 (Easy)
 *
 * Time: O(m*n), Space: O(m*n) recursion stack in worst case
 */
public class Ques1_FloodFill {

    public static int[][] floodFill(int[][] image, int sr, int sc, int newColor) {
        int originalColor = image[sr][sc];
        if (originalColor != newColor) {
            dfs(image, sr, sc, originalColor, newColor);
        }
        return image;
    }

    private static void dfs(int[][] image, int r, int c, int originalColor, int newColor) {
        if (r < 0 || r >= image.length || c < 0 || c >= image[0].length) return;
        if (image[r][c] != originalColor) return;

        image[r][c] = newColor;
        dfs(image, r + 1, c, originalColor, newColor);
        dfs(image, r - 1, c, originalColor, newColor);
        dfs(image, r, c + 1, originalColor, newColor);
        dfs(image, r, c - 1, originalColor, newColor);
    }

    static void main() {
        int[][] image = {{1, 1, 1}, {1, 1, 0}, {1, 0, 1}};
        floodFill(image, 1, 1, 2);
        // [[2,2,2],[2,2,0],[2,0,1]]
        for (int[] row : image) System.out.println(Arrays.toString(row));
    }
}
