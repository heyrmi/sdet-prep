package ra.hul.dsa.bitmanipulation;

import java.util.Arrays;

/**
 * Counting Bits - popcount of every number 0..n via the DP ans[i] = ans[i>>1] + (i&1).
 * LeetCode #338 (Easy)
 *
 * Time: O(n), Space: O(n)
 */
public class Ques3_CountingBits {

    public static int[] countBits(int n) {
        int[] ans = new int[n + 1];     // indices 0..n inclusive
        for (int i = 1; i <= n; i++) {
            ans[i] = ans[i >> 1] + (i & 1);  // drop lowest bit (smaller, known) + add it back
        }
        return ans;
    }

    static void main() {
        System.out.println(Arrays.toString(countBits(2)));   // [0, 1, 1]
        System.out.println(Arrays.toString(countBits(5)));   // [0, 1, 1, 2, 1, 2]
    }
}
