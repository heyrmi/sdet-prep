package ra.hul.dsa.monotonicstack;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Next Greater Element II - Next greater number for each element in a circular array.
 * LeetCode #503 (Medium)
 *
 * Simulate circularity by iterating 2n times over indices i % n. A monotonic decreasing stack
 * of indices holds elements still waiting for their next-greater; each new element resolves them.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques2_NextGreaterElementII {

    public static int[] nextGreaterElements(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        Deque<Integer> stack = new ArrayDeque<>(); // indices, values decreasing down the stack
        for (int i = 0; i < 2 * n; i++) {
            int val = nums[i % n];
            while (!stack.isEmpty() && nums[stack.peek()] < val) {
                result[stack.pop()] = val;
            }
            if (i < n) stack.push(i); // only push real indices in the first pass
        }
        return result;
    }

    static void main() {
        int[][] inputs = {
            {1, 2, 1},
            {1, 2, 3, 4, 3},
            {5, 4, 3, 2, 1},
            {1, 1, 1}
        };
        int[][] expected = {
            {2, -1, 2},
            {2, 3, 4, -1, 4},
            {-1, 5, 5, 5, 5},
            {-1, -1, -1}
        };
        boolean ok = true;
        for (int i = 0; i < inputs.length; i++) {
            int[] got = nextGreaterElements(inputs[i]);
            System.out.println(Arrays.toString(got) + " expected " + Arrays.toString(expected[i]));
            ok &= Arrays.equals(got, expected[i]);
        }
        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
