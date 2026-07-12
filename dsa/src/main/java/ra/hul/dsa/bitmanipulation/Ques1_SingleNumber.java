package ra.hul.dsa.bitmanipulation;

/**
 * Single Number - every element appears twice except one; find it with XOR cancellation.
 * LeetCode #136 (Easy)
 *
 * Time: O(n), Space: O(1)
 */
public class Ques1_SingleNumber {

    public static int singleNumber(int[] nums) {
        int acc = 0;            // 0 is the XOR identity
        for (int v : nums) {
            acc ^= v;           // pairs cancel to 0; the unique value remains
        }
        return acc;
    }

    static void main() {
        System.out.println(singleNumber(new int[]{2, 2, 1}));         // 1
        System.out.println(singleNumber(new int[]{4, 1, 2, 1, 2}));   // 4
        System.out.println(singleNumber(new int[]{7}));               // 7
    }
}
