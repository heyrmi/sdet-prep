package ra.hul.dsa.bitmanipulation;

/**
 * Single Number II - every element appears three times except one; recover it by counting bits mod 3.
 * LeetCode #137 (Medium)
 *
 * Time: O(32n) = O(n), Space: O(1)
 */
public class Ques7_SingleNumberII {

    public static int singleNumber(int[] nums) {
        int answer = 0;
        for (int i = 0; i < 32; i++) {
            int count = 0;
            for (int v : nums) {
                count += (v >>> i) & 1;   // logical shift: read bit i regardless of sign
            }
            if (count % 3 != 0) {
                answer |= (1 << i);       // i==31 sets the sign bit — correct for negatives
            }
        }
        return answer;
    }

    static void main() {
        System.out.println(singleNumber(new int[]{2, 2, 3, 2}));                            // 3
        System.out.println(singleNumber(new int[]{0, 1, 0, 1, 0, 1, 99}));                  // 99
        System.out.println(singleNumber(new int[]{-2, -2, 1, 1, -3, 1, -3, -3, -2, -4}));   // -4
    }
}
