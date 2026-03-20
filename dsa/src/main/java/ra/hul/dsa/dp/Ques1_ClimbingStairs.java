package ra.hul.dsa.dp;

/**
 * Climbing Stairs - How many distinct ways to climb n stairs (1 or 2 steps at a time)?
 * LeetCode #70 (Easy)
 *
 * Time: O(n), Space: O(1) - optimized to use two variables instead of array
 */
public class Ques1_ClimbingStairs {

    public static int climbStairs(int n) {
        if (n <= 2) return n;
        int prev2 = 1, prev1 = 2;
        for (int i = 3; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        return prev1;
    }

    static void main() {
        System.out.println(climbStairs(2));  // 2  (1+1, 2)
        System.out.println(climbStairs(3));  // 3  (1+1+1, 1+2, 2+1)
        System.out.println(climbStairs(5));  // 8
        System.out.println(climbStairs(10)); // 89
    }
}
