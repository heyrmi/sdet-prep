package ra.hul.dsa.slidingwindow;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Sliding Window Maximum - maximum of every window of size k, using a monotonic deque.
 *
 * Time: O(n), Space: O(k)
 */
public class Ques9_SlidingWindowMaximum {

    public static int[] maxSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new ArrayDeque<>(); // holds INDICES, values decreasing front->back

        for (int right = 0; right < n; right++) {
            // 1) Drop indices that have fallen out of the window on the left.
            if (!deque.isEmpty() && deque.peekFirst() <= right - k) {
                deque.pollFirst();
            }
            // 2) Drop smaller/equal values from the back; they can never be the max again.
            while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[right]) {
                deque.pollLast();
            }
            // 3) Add the current index.
            deque.offerLast(right);
            // 4) Once the first full window is formed, the front is its maximum.
            if (right >= k - 1) {
                result[right - k + 1] = nums[deque.peekFirst()];
            }
        }
        return result;
    }

    static void main() {
        System.out.println(Arrays.toString(maxSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3))); // [3, 3, 5, 5, 6, 7]
        System.out.println(Arrays.toString(maxSlidingWindow(new int[]{1}, 1)));                          // [1]
        System.out.println(Arrays.toString(maxSlidingWindow(new int[]{9, 8, 7, 6}, 2)));                 // [9, 8, 7]
    }
}
