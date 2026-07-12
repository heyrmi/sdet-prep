package ra.hul.dsa.stack;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Daily Temperatures - For each day, how many days until a warmer temperature, using a monotonic decreasing stack.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques5_DailyTemperatures {

    public static int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] answer = new int[n]; // defaults to 0 = "no warmer day"
        Deque<Integer> stack = new ArrayDeque<>(); // indices, temps decreasing down the stack
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int j = stack.pop();
                answer[j] = i - j;
            }
            stack.push(i);
        }
        return answer;
    }

    static void main() {
        System.out.println(Arrays.toString(dailyTemperatures(new int[]{73, 74, 75, 71, 69, 72, 76, 73}))); // [1, 1, 4, 2, 1, 1, 0, 0]
        System.out.println(Arrays.toString(dailyTemperatures(new int[]{30, 40, 50, 60})));                 // [1, 1, 1, 0]
        System.out.println(Arrays.toString(dailyTemperatures(new int[]{30, 60, 90})));                     // [1, 1, 0]
        System.out.println(Arrays.toString(dailyTemperatures(new int[]{90, 80, 70, 60})));                 // [0, 0, 0, 0]
    }
}
