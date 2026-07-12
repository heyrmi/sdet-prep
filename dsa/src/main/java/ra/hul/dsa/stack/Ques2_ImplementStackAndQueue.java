package ra.hul.dsa.stack;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Implement Stack and Queue - LIFO stack and FIFO queue backed by a raw int[] (circular buffer for the queue).
 *
 * Time: O(1) amortized per operation, Space: O(n)
 */
public class Ques2_ImplementStackAndQueue {

    /** LIFO stack backed by a doubling int[]. Top = last element written. */
    static class ArrayStack {
        private int[] data = new int[4];
        private int size = 0;

        public void push(int x) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = x;
        }

        public int pop() {
            if (size == 0) throw new NoSuchElementException();
            return data[--size];
        }

        public int peek() {
            if (size == 0) throw new NoSuchElementException();
            return data[size - 1];
        }

        public boolean isEmpty() { return size == 0; }

        public int size() { return size; }
    }

    /** FIFO queue backed by a circular buffer. head = front index, count = live elements. */
    static class ArrayQueue {
        private int[] data = new int[4];
        private int head = 0;
        private int count = 0;

        public void enqueue(int x) {
            if (count == data.length) grow();
            int tail = (head + count) % data.length;
            data[tail] = x;
            count++;
        }

        public int dequeue() {
            if (count == 0) throw new NoSuchElementException();
            int v = data[head];
            head = (head + 1) % data.length;
            count--;
            return v;
        }

        public int peek() {
            if (count == 0) throw new NoSuchElementException();
            return data[head];
        }

        public boolean isEmpty() { return count == 0; }

        public int size() { return count; }

        // Copy live elements out in logical order (from head), then reset head = 0.
        private void grow() {
            int[] bigger = new int[data.length * 2];
            for (int i = 0; i < count; i++) {
                bigger[i] = data[(head + i) % data.length];
            }
            data = bigger;
            head = 0;
        }
    }

    static void main() {
        ArrayStack st = new ArrayStack();
        st.push(1);
        st.push(2);
        st.push(3);
        System.out.println(st.peek());    // 3
        System.out.println(st.pop());     // 3
        System.out.println(st.pop());     // 2
        System.out.println(st.size());    // 1
        System.out.println(st.isEmpty()); // false

        ArrayQueue q = new ArrayQueue();
        q.enqueue(10);
        q.enqueue(20);
        q.enqueue(30);
        System.out.println(q.dequeue());  // 10
        System.out.println(q.peek());     // 20
        q.enqueue(40);
        q.enqueue(50);
        q.enqueue(60);                    // forces a grow with wraparound
        System.out.println(q.dequeue());  // 20
        System.out.println(q.dequeue());  // 30
        System.out.println(q.size());     // 3
    }
}
