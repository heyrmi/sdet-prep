package ra.hul.dsa.heap;

import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Last Stone Weight - repeatedly smash the two heaviest stones using a max-heap.
 * LeetCode #1046 (Easy)
 *
 * Time: O(n log n), Space: O(n)
 */
public class Ques5_LastStoneWeight {

    public static int lastStoneWeight(int[] stones) {
        PriorityQueue<Integer> max = new PriorityQueue<>(Collections.reverseOrder()); // max-heap
        for (int s : stones) max.offer(s);
        while (max.size() >= 2) {
            int y = max.poll(); // heaviest
            int x = max.poll(); // next heaviest, x <= y
            if (x != y) max.offer(y - x); // survivor; if equal, both are destroyed
        }
        return max.isEmpty() ? 0 : max.peek();
    }

    static void main() {
        System.out.println(lastStoneWeight(new int[]{2, 7, 4, 1, 8, 1})); // 1
        System.out.println(lastStoneWeight(new int[]{1}));                // 1
        System.out.println(lastStoneWeight(new int[]{2, 2}));             // 0
    }
}
