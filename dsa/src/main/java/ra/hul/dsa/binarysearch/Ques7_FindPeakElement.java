package ra.hul.dsa.binarysearch;

/**
 * Find Peak Element - follow the uphill slope; binary search without a sorted array.
 * LeetCode #162 (Medium)
 *
 * Time: O(log n), Space: O(1)
 */
public class Ques7_FindPeakElement {

    public static int findPeakElement(int[] nums) {
        int lo = 0, hi = nums.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] < nums[mid + 1]) lo = mid + 1;   // rising slope: peak to the right
            else hi = mid;                                 // falling: mid or left is a peak
        }
        return lo;
    }

    static void main() {
        System.out.println(findPeakElement(new int[]{1, 2, 3, 1}));             // 2
        System.out.println(findPeakElement(new int[]{1, 2, 1, 3, 5, 6, 4}));    // 5
        System.out.println(findPeakElement(new int[]{1}));                      // 0
        System.out.println(findPeakElement(new int[]{1, 2}));                   // 1
    }
}
