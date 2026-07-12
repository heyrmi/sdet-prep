package ra.hul.dsa.arrays;

import java.util.Arrays;

/**
 * Move Zeroes - move all 0s to the end in place, preserving the order of non-zero elements.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques14_MoveZeroes {

    public static void moveZeroes(int[] nums) {
        int insert = 0; // next slot for a non-zero element
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                int tmp = nums[insert];
                nums[insert] = nums[i];
                nums[i] = tmp;
                insert++;
            }
        }
    }

    static void main() {
        int[] a = {0, 1, 0, 3, 12};
        moveZeroes(a);
        System.out.println(Arrays.toString(a)); // [1, 3, 12, 0, 0]

        int[] b = {0};
        moveZeroes(b);
        System.out.println(Arrays.toString(b)); // [0]

        int[] c = {1, 2, 3};
        moveZeroes(c);
        System.out.println(Arrays.toString(c)); // [1, 2, 3]

        int[] d = {0, 0, 1};
        moveZeroes(d);
        System.out.println(Arrays.toString(d)); // [1, 0, 0]
    }
}
