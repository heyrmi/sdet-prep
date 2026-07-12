package ra.hul.dsa.matrix;

import java.util.Arrays;

/**
 * Plus One - increment a large integer represented as a digit array by one.
 *
 * Time: O(n), Space: O(n) - grade-school carry from least significant digit
 */
public class Ques5_PlusOne {

    public static int[] plusOne(int[] digits) {
        for (int i = digits.length - 1; i >= 0; i--) {
            if (digits[i] < 9) {
                digits[i]++;          // no carry: increment and we're done
                return digits;
            }
            digits[i] = 0;            // 9 -> 0, carry continues left
        }
        // All digits were 9: result is 1 followed by zeros (one digit longer).
        int[] grown = new int[digits.length + 1]; // zero-filled
        grown[0] = 1;
        return grown;
    }

    static void main() {
        System.out.println(Arrays.toString(plusOne(new int[]{1, 2, 3})));    // [1, 2, 4]
        System.out.println(Arrays.toString(plusOne(new int[]{4, 3, 2, 1}))); // [4, 3, 2, 2]
        System.out.println(Arrays.toString(plusOne(new int[]{9})));          // [1, 0]
        System.out.println(Arrays.toString(plusOne(new int[]{9, 9, 9})));    // [1, 0, 0, 0]
    }
}
