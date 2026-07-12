package ra.hul.dsa.twopointers;

import java.util.Arrays;

/**
 * Remove Duplicates from Sorted Array - compact unique values in place; return the count of uniques.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques4_RemoveDuplicatesFromSortedArray {

    public static int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;
        int slow = 0; // last committed unique value sits at nums[slow]
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                slow++;
                nums[slow] = nums[fast];
            }
        }
        return slow + 1; // count of uniques
    }

    static void main() {
        int[] a = {1, 1, 2};
        int k = removeDuplicates(a);
        System.out.println(k);                              // 2
        System.out.println(Arrays.toString(Arrays.copyOf(a, k))); // [1, 2]

        int[] b = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        int k2 = removeDuplicates(b);
        System.out.println(k2);                             // 5
        System.out.println(Arrays.toString(Arrays.copyOf(b, k2))); // [0, 1, 2, 3, 4]
    }
}
