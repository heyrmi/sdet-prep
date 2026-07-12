package ra.hul.dsa.twopointers;

import java.util.Arrays;

/**
 * Sort Colors - sort an array of 0s, 1s, 2s in place in one pass (Dutch National Flag).
 *
 * Time: O(n), Space: O(1)
 */
public class Ques5_SortColors {

    public static void sortColors(int[] nums) {
        int low = 0, mid = 0, high = nums.length - 1;
        while (mid <= high) {
            switch (nums[mid]) {
                case 0 -> { swap(nums, low, mid); low++; mid++; }
                case 1 -> mid++;
                default -> { swap(nums, mid, high); high--; } // case 2: do not advance mid
            }
        }
    }

    private static void swap(int[] a, int i, int j) {
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    static void main() {
        int[] a = {2, 0, 2, 1, 1, 0};
        sortColors(a);
        System.out.println(Arrays.toString(a)); // [0, 0, 1, 1, 2, 2]

        int[] b = {2, 0, 1};
        sortColors(b);
        System.out.println(Arrays.toString(b)); // [0, 1, 2]

        int[] c = {1, 1, 1};
        sortColors(c);
        System.out.println(Arrays.toString(c)); // [1, 1, 1]
    }
}
