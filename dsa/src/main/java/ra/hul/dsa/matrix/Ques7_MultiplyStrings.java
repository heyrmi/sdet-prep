package ra.hul.dsa.matrix;

/**
 * Multiply Strings - multiply two non-negative integers given as strings, return the product string.
 *
 * Time: O(m * n), Space: O(m + n) - grade-school multiplication on a digit array
 */
public class Ques7_MultiplyStrings {

    public static String multiply(String num1, String num2) {
        if (num1.equals("0") || num2.equals("0")) return "0";

        int m = num1.length(), n = num2.length();
        int[] result = new int[m + n];   // zero-filled; product has at most m+n digits

        for (int i = m - 1; i >= 0; i--) {
            int d1 = num1.charAt(i) - '0';
            for (int j = n - 1; j >= 0; j--) {
                int d2 = num2.charAt(j) - '0';
                int p1 = i + j, p2 = i + j + 1;   // high (carry/tens), low (ones)
                int sum = d1 * d2 + result[p2];
                result[p2] = sum % 10;
                result[p1] += sum / 10;           // propagate carry into the high slot
            }
        }

        // Join digits, skipping any leading zeros.
        StringBuilder sb = new StringBuilder();
        for (int d : result) {
            if (sb.length() == 0 && d == 0) continue;  // skip leading zeros
            sb.append((char) ('0' + d));
        }
        return sb.length() == 0 ? "0" : sb.toString();
    }

    static void main() {
        System.out.println(multiply("2", "3"));     // 6
        System.out.println(multiply("123", "456")); // 56088
        System.out.println(multiply("0", "12345")); // 0
        System.out.println(multiply("999", "999")); // 998001
    }
}
