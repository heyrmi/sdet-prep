package ra.hul.dsa.stack;

import java.util.Map;
import java.util.Stack;

/**
 * Valid Parentheses - Check if a string of brackets is valid.
 * LeetCode #20 (Easy)
 *
 * Time: O(n), Space: O(n)
 */
public class Ques1_ValidParentheses {

    public static boolean isValid(String s) {
        Stack<Character> stack = new Stack<>();
        Map<Character, Character> pairs = Map.of(')', '(', '}', '{', ']', '[');

        for (char c : s.toCharArray()) {
            if (pairs.containsValue(c)) {
                stack.push(c);
            } else if (pairs.containsKey(c)) {
                if (stack.isEmpty() || stack.pop() != pairs.get(c)) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    static void main() {
        System.out.println(isValid("()"));       // true
        System.out.println(isValid("()[]{}"));   // true
        System.out.println(isValid("(]"));       // false
        System.out.println(isValid("([])"));     // true
        System.out.println(isValid("{[]}"));     // true
    }
}
