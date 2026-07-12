package ra.hul.dsa.stack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Min Stack - A stack that also reports its current minimum in O(1) via a parallel stack of running minimums.
 *
 * Time: O(1) per operation, Space: O(n)
 */
public class Ques3_MinStack {

    /** Stack with O(1) getMin via a parallel stack of running minimums. */
    static class MinStack {
        private final Deque<Integer> data = new ArrayDeque<>();
        private final Deque<Integer> mins = new ArrayDeque<>();

        public void push(int x) {
            data.push(x);
            int newMin = mins.isEmpty() ? x : Math.min(x, mins.peek());
            mins.push(newMin);
        }

        public void pop() {
            data.pop();
            mins.pop();
        }

        public int top() {
            return data.peek();
        }

        public int getMin() {
            return mins.peek();
        }
    }

    static void main() {
        MinStack st = new MinStack();
        st.push(-2);
        st.push(0);
        st.push(-3);
        System.out.println(st.getMin()); // -3
        st.pop();
        System.out.println(st.top());    // 0
        System.out.println(st.getMin()); // -2
    }
}
