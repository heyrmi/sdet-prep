package ra.hul.dsa.heap;

import java.util.PriorityQueue;

/**
 * Kth Largest Element in a Stream - size-k min-heap whose root is always the Kth largest.
 * LeetCode #703 (Easy)
 *
 * Time: add O(log k), Space: O(k)
 */
public class Ques7_KthLargestElementInAStream {

    static class KthLargest {
        private final int k;
        private final PriorityQueue<Integer> heap; // min-heap of the k largest seen so far

        KthLargest(int k, int[] nums) {
            this.k = k;
            this.heap = new PriorityQueue<>(); // min-heap by default
            for (int x : nums) add(x);
        }

        int add(int val) {
            heap.offer(val);
            if (heap.size() > k) heap.poll(); // drop the smallest -> keep only the k largest
            return heap.peek();               // root = Kth largest (guaranteed >= k elements present)
        }
    }

    static void main() {
        KthLargest k = new KthLargest(3, new int[]{4, 5, 8, 2});
        System.out.println(k.add(3));  // 4
        System.out.println(k.add(5));  // 5
        System.out.println(k.add(10)); // 5
        System.out.println(k.add(9));  // 8
        System.out.println(k.add(4));  // 8
    }
}
