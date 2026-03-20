package ra.hul.dsa.recursion;

/**
 * Fibonacci Number - Return the nth Fibonacci number.
 * LeetCode #509 (Easy)
 *
 * Shown with both recursive (for learning) and iterative (optimal) approaches.
 */
public class Ques1_Fibonacci {

    // Approach 1: Pure recursion - Time: O(2^n), Space: O(n) - for understanding only
    public static int fibRecursive(int n) {
        if (n <= 1) return n;
        return fibRecursive(n - 1) + fibRecursive(n - 2);
    }

    // Approach 2: Iterative - Time: O(n), Space: O(1) - optimal
    public static int fibIterative(int n) {
        if (n <= 1) return n;
        int prev2 = 0, prev1 = 1;
        for (int i = 2; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        return prev1;
    }

    public static void main(String[] args) {
        for (int i = 0; i <= 10; i++) {
            System.out.printf("fib(%d) = %d (recursive) | %d (iterative)%n",
                    i, fibRecursive(i), fibIterative(i));
        }
        // fib(10) = 55
    }
}
