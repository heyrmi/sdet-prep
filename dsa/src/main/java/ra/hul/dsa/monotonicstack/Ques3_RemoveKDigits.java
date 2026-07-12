package ra.hul.dsa.monotonicstack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Remove K Digits - Remove k digits from num so the remaining number is the smallest possible.
 * LeetCode #402 (Medium)
 *
 * Greedy with a monotonic increasing stack: whenever the current digit is smaller than the top,
 * popping the top yields a smaller number, so pop while budget k remains. Any leftover k trims
 * from the tail. Strip leading zeros at the end.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques3_RemoveKDigits {

    public static String removeKdigits(String num, int k) {
        Deque<Character> stack = new ArrayDeque<>(); // digits, non-decreasing from bottom to top
        for (char c : num.toCharArray()) {
            while (k > 0 && !stack.isEmpty() && stack.peek() > c) {
                stack.pop();
                k--;
            }
            stack.push(c);
        }
        // remove any remaining from the top (largest tail digits)
        while (k > 0 && !stack.isEmpty()) {
            stack.pop();
            k--;
        }
        // stack bottom->top is the number; build it and strip leading zeros
        StringBuilder sb = new StringBuilder();
        for (char c : stack) sb.append(c); // ArrayDeque iterates top->bottom
        sb.reverse();
        int i = 0;
        while (i < sb.length() - 1 && sb.charAt(i) == '0') i++;
        String res = sb.substring(i);
        return res.isEmpty() ? "0" : res;
    }

    static void main() {
        String[][] cases = {
            {"1432219", "3", "1219"},
            {"10200", "1", "200"},
            {"10", "2", "0"},
            {"112", "1", "11"},
            {"1234567890", "9", "0"}
        };
        boolean ok = true;
        for (String[] c : cases) {
            String got = removeKdigits(c[0], Integer.parseInt(c[1]));
            System.out.println("removeKdigits(" + c[0] + ", " + c[1] + ") = " + got + " expected " + c[2]);
            ok &= got.equals(c[2]);
        }
        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
