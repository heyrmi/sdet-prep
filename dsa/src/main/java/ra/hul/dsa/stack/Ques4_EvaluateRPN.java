package ra.hul.dsa.stack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Evaluate Reverse Polish Notation - Evaluate a postfix arithmetic expression using a stack.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques4_EvaluateRPN {

    public static int evalRPN(String[] tokens) {
        Deque<Integer> st = new ArrayDeque<>();
        for (String token : tokens) {
            switch (token) {
                case "+" -> { int b = st.pop(), a = st.pop(); st.push(a + b); }
                case "-" -> { int b = st.pop(), a = st.pop(); st.push(a - b); }
                case "*" -> { int b = st.pop(), a = st.pop(); st.push(a * b); }
                case "/" -> { int b = st.pop(), a = st.pop(); st.push(a / b); } // truncates toward zero
                default  -> st.push(Integer.parseInt(token));
            }
        }
        return st.pop();
    }

    static void main() {
        System.out.println(evalRPN(new String[]{"2", "1", "+", "3", "*"}));  // 9
        System.out.println(evalRPN(new String[]{"4", "13", "5", "/", "+"})); // 6
        System.out.println(evalRPN(new String[]{
                "10", "6", "9", "3", "+", "-11", "*", "/", "*", "17", "+", "5", "+"})); // 22
    }
}
