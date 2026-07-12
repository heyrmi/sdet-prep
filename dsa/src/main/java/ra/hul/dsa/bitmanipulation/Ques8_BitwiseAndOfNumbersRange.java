package ra.hul.dsa.bitmanipulation;

/**
 * Bitwise AND of Numbers Range - AND of all integers in [left, right] equals their common bit prefix.
 * LeetCode #201 (Medium)
 *
 * Time: O(log right), Space: O(1)
 */
public class Ques8_BitwiseAndOfNumbersRange {

    public static int rangeBitwiseAnd(int left, int right) {
        int shifts = 0;
        while (left != right) {     // collapse to the common prefix
            left >>>= 1;
            right >>>= 1;
            shifts++;
        }
        return left << shifts;      // restore the prefix's position
    }

    static void main() {
        System.out.println(rangeBitwiseAnd(5, 7));            // 4
        System.out.println(rangeBitwiseAnd(0, 0));            // 0
        System.out.println(rangeBitwiseAnd(1, 2147483647));  // 0
        System.out.println(rangeBitwiseAnd(12, 15));         // 12
    }
}
