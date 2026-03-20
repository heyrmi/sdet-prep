package ra.hul.dsa.heap;

import java.util.PriorityQueue;

/**
 * Kth Largest Element in an Array - Find the kth largest using a min-heap.
 * LeetCode #215 (Medium, but heap usage is a fundamental Easy concept)
 *
 * Time: O(n log k), Space: O(k)
 */
public class Ques1_KthLargestElement {

    public static int findKthLargest(int[] nums, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        for (int num : nums) {
            minHeap.offer(num);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }
        return minHeap.peek();
    }

    public static void main(String[] args) {
        System.out.println(findKthLargest(new int[]{3, 2, 1, 5, 6, 4}, 2));    // 5
        System.out.println(findKthLargest(new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6}, 4)); // 4
    }
}
