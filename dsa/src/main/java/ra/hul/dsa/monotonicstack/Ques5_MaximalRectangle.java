package ra.hul.dsa.monotonicstack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maximal Rectangle - Largest rectangle of 1s in a binary matrix.
 * LeetCode #85 (Hard)
 *
 * Build a running histogram per row (consecutive 1s in each column), then reuse the
 * Largest-Rectangle-in-Histogram monotonic-stack routine (see Ques1) on each row's histogram.
 *
 * Time: O(rows * cols), Space: O(cols)
 */
public class Ques5_MaximalRectangle {

    public static int maximalRectangle(char[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) return 0;
        int cols = matrix[0].length;
        int[] heights = new int[cols];
        int best = 0;
        for (char[] row : matrix) {
            for (int c = 0; c < cols; c++) {
                heights[c] = (row[c] == '1') ? heights[c] + 1 : 0;
            }
            best = Math.max(best, largestRectangleArea(heights));
        }
        return best;
    }

    // same monotonic-stack histogram routine as Ques1
    private static int largestRectangleArea(int[] heights) {
        int n = heights.length;
        Deque<Integer> stack = new ArrayDeque<>();
        int best = 0;
        for (int i = 0; i <= n; i++) {
            int curr = (i == n) ? 0 : heights[i];
            while (!stack.isEmpty() && curr < heights[stack.peek()]) {
                int height = heights[stack.pop()];
                int left = stack.isEmpty() ? -1 : stack.peek();
                best = Math.max(best, height * (i - left - 1));
            }
            stack.push(i);
        }
        return best;
    }

    static void main() {
        char[][] m1 = {
            {'1', '0', '1', '0', '0'},
            {'1', '0', '1', '1', '1'},
            {'1', '1', '1', '1', '1'},
            {'1', '0', '0', '1', '0'}
        };
        char[][] m2 = {{'0'}};
        char[][] m3 = {{'1'}};
        char[][] m4 = {
            {'1', '1'},
            {'1', '1'}
        };

        boolean ok = true;
        int r1 = maximalRectangle(m1);
        System.out.println("area=" + r1 + " expected 6");
        ok &= r1 == 6;
        int r2 = maximalRectangle(m2);
        System.out.println("area=" + r2 + " expected 0");
        ok &= r2 == 0;
        int r3 = maximalRectangle(m3);
        System.out.println("area=" + r3 + " expected 1");
        ok &= r3 == 1;
        int r4 = maximalRectangle(m4);
        System.out.println("area=" + r4 + " expected 4");
        ok &= r4 == 4;

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
