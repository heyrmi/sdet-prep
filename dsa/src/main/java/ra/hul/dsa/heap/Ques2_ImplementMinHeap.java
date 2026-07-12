package ra.hul.dsa.heap;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Implement a Binary Min-Heap - array-backed heap with insert/peek/extractMin/size.
 *
 * Time: insert/extractMin O(log n), peek/size O(1); Space: O(n)
 */
public class Ques2_ImplementMinHeap {

    static class MinHeap {
        private int[] heap = new int[8];
        private int size = 0;

        public void insert(int x) {
            if (size == heap.length) heap = Arrays.copyOf(heap, heap.length * 2);
            heap[size] = x;
            siftUp(size);
            size++;
        }

        public int peek() {
            if (size == 0) throw new NoSuchElementException("heap is empty");
            return heap[0];
        }

        public int extractMin() {
            if (size == 0) throw new NoSuchElementException("heap is empty");
            int min = heap[0];
            size--;
            heap[0] = heap[size];     // move last element to the root
            if (size > 0) siftDown(0);
            return min;
        }

        public int size() {
            return size;
        }

        private void siftUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (heap[i] >= heap[parent]) break; // parent already <= node: done
                swap(i, parent);
                i = parent;
            }
        }

        private void siftDown(int i) {
            while (true) {
                int left = 2 * i + 1;
                int right = 2 * i + 2;
                int smallest = i;
                if (left < size && heap[left] < heap[smallest]) smallest = left;
                if (right < size && heap[right] < heap[smallest]) smallest = right;
                if (smallest == i) break; // no child smaller than node: done
                swap(i, smallest);
                i = smallest;
            }
        }

        private void swap(int i, int j) {
            int t = heap[i];
            heap[i] = heap[j];
            heap[j] = t;
        }
    }

    static void main() {
        MinHeap h = new MinHeap();
        for (int x : new int[]{5, 3, 8, 1, 4}) h.insert(x);
        System.out.println(h.size());        // 5
        System.out.println(h.peek());        // 1
        System.out.println(h.extractMin());  // 1
        System.out.println(h.peek());        // 3
        System.out.println(h.extractMin());  // 3
        System.out.println(h.peek());        // 4
    }
}
