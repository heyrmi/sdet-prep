package ra.hul.dsa.heap;

import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Find Median from Data Stream - two heaps keep the running median instantly reachable.
 * LeetCode #295 (Hard)
 *
 * Time: addNum O(log n), findMedian O(1); Space: O(n)
 */
public class Ques4_FindMedianFromDataStream {

    static class MedianFinder {
        private final PriorityQueue<Integer> low  = new PriorityQueue<>(Collections.reverseOrder()); // max-heap, smaller half
        private final PriorityQueue<Integer> high = new PriorityQueue<>();                            // min-heap, larger half

        public void addNum(int num) {
            low.offer(num);              // tentatively add to the lower half
            high.offer(low.poll());      // shuffle its max up to the higher half (keeps order between heaps)
            if (high.size() > low.size())
                low.offer(high.poll());  // rebalance: keep the extra element in low
        }

        public double findMedian() {
            if (low.size() > high.size()) return low.peek();          // odd count
            return ((double) low.peek() + high.peek()) / 2.0;         // even count: average the two roots
        }
    }

    static void main() {
        MedianFinder mf = new MedianFinder();
        mf.addNum(1);
        System.out.println(mf.findMedian()); // 1.0
        mf.addNum(2);
        System.out.println(mf.findMedian()); // 1.5
        mf.addNum(3);
        System.out.println(mf.findMedian()); // 2.0
        mf.addNum(4);
        System.out.println(mf.findMedian()); // 2.5
    }
}
