package ra.hul.dsa.bitmanipulation;

/**
 * Missing Number - find the missing value in 0..n via index/value XOR cancellation.
 * LeetCode #268 (Easy)
 *
 * Time: O(n), Space: O(1)
 */
public class Ques5_MissingNumber {

    public static int missingNumber(int[] nums) {
        int acc = nums.length;          // fold in index n (no array slot for it)
        for (int i = 0; i < nums.length; i++) {
            acc ^= i ^ nums[i];         // index and value cancel for present numbers
        }
        return acc;                     // the missing index survives
    }

    static void main() {
        System.out.println(missingNumber(new int[]{3, 0, 1}));                        // 2
        System.out.println(missingNumber(new int[]{0, 1}));                           // 2
        System.out.println(missingNumber(new int[]{9, 6, 4, 2, 3, 5, 7, 0, 1}));      // 8
        System.out.println(missingNumber(new int[]{0}));                              // 1
    }
}
