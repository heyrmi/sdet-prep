package ra.hul.dsa.queue;

import java.util.Stack;

/**
 * Implement Queue using Stacks - Implement FIFO queue using two LIFO stacks.
 * LeetCode #232 (Easy)
 *
 * Amortized O(1) per operation, Space: O(n)
 */
public class Ques1_QueueUsingStacks {

    private final Stack<Integer> pushStack = new Stack<>();
    private final Stack<Integer> popStack = new Stack<>();

    public void push(int x) {
        pushStack.push(x);
    }

    public int pop() {
        shiftStacks();
        return popStack.pop();
    }

    public int peek() {
        shiftStacks();
        return popStack.peek();
    }

    public boolean empty() {
        return pushStack.isEmpty() && popStack.isEmpty();
    }

    private void shiftStacks() {
        if (popStack.isEmpty()) {
            while (!pushStack.isEmpty()) {
                popStack.push(pushStack.pop());
            }
        }
    }

    public static void main(String[] args) {
        Ques1_QueueUsingStacks queue = new Ques1_QueueUsingStacks();
        queue.push(1);
        queue.push(2);
        System.out.println(queue.peek());  // 1
        System.out.println(queue.pop());   // 1
        System.out.println(queue.empty()); // false
        System.out.println(queue.pop());   // 2
        System.out.println(queue.empty()); // true
    }
}
