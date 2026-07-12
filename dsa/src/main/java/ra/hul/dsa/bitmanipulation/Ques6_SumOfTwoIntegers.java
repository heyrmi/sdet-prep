package ra.hul.dsa.bitmanipulation;

/**
 * Sum of Two Integers - add without + or - using XOR (sum) and AND-shift (carry).
 * LeetCode #371 (Medium)
 *
 * Time: O(1), Space: O(1)
 */
public class Ques6_SumOfTwoIntegers {

    public static int getSum(int a, int b) {
        while (b != 0) {                // b carries the running carry
            int carry = (a & b) << 1;   // carry from old a, b (lands one column up)
            a = a ^ b;                  // sum ignoring carry
            b = carry;                  // fold the carry back in next round
        }
        return a;
    }

    static void main() {
        System.out.println(getSum(1, 2));    // 3
        System.out.println(getSum(2, 3));    // 5
        System.out.println(getSum(-2, 3));   // 1
        System.out.println(getSum(-1, 1));   // 0
    }
}
