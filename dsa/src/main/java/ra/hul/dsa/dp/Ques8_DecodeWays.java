package ra.hul.dsa.dp;

/**
 * Decode Ways - Number of ways to decode a digit string (A=1..Z=26).
 *
 * Time: O(n), Space: O(1) - add dp[i-1] if single digit valid, dp[i-2] if the pair is in 10..26.
 */
public class Ques8_DecodeWays {

    public static int numDecodings(String s) {
        int n = s.length();
        if (n == 0 || s.charAt(0) == '0') return 0;

        int prev2 = 1;                       // dp[i-2], starts as dp[0] = 1 (empty string)
        int prev1 = 1;                       // dp[i-1], dp[1] = 1 (s[0] is non-zero here)
        for (int i = 2; i <= n; i++) {
            int cur = 0;
            if (s.charAt(i - 1) != '0') {    // single digit 1..9
                cur += prev1;
            }
            int two = (s.charAt(i - 2) - '0') * 10 + (s.charAt(i - 1) - '0');
            if (two >= 10 && two <= 26) {    // two-digit 10..26
                cur += prev2;
            }
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    static void main() {
        System.out.println(numDecodings("12"));  // 2
        System.out.println(numDecodings("226")); // 3
        System.out.println(numDecodings("06"));  // 0
        System.out.println(numDecodings("10"));  // 1
    }
}
