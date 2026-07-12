package ra.hul.dsa.monotonicstack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Largest Rectangle in Histogram - Max-area rectangle bounded by contiguous bars.
 * LeetCode #84 (Hard)
 *
 * Keep a stack of indices with increasing heights. When a shorter bar arrives, pop taller bars:
 * the popped bar's rectangle extends from the new left boundary (stack top after pop) to i-1.
 * A trailing sentinel height of 0 flushes the stack.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques1_LargestRectangleInHistogram {

    public static int largestRectangleArea(int[] heights) {
        int n = heights.length;
        Deque<Integer> stack = new ArrayDeque<>(); // indices, heights increasing down the stack
        int best = 0;
        for (int i = 0; i <= n; i++) {
            int curr = (i == n) ? 0 : heights[i]; // sentinel 0 at the end flushes everything
            while (!stack.isEmpty() && curr < heights[stack.peek()]) {
                int height = heights[stack.pop()];
                int leftBoundary = stack.isEmpty() ? -1 : stack.peek();
                int width = i - leftBoundary - 1;
                best = Math.max(best, height * width);
            }
            stack.push(i);
        }
        return best;
    }

    static void main() {
        int[][] inputs = {
            {2, 1, 5, 6, 2, 3},
            {2, 4},
            {2, 1, 2},
            {0},
            {6, 2, 5, 4, 5, 1, 6}
        };
        int[] expected = {10, 4, 3, 0, 12};
        boolean ok = true;
        for (int i = 0; i < inputs.length; i++) {
            int got = largestRectangleArea(inputs[i]);
            System.out.println("area=" + got + " expected=" + expected[i]);
            ok &= got == expected[i];
        }
        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
