package ra.hul.dsa.binarysearch;

import java.util.Arrays;

/**
 * Find First and Last Position of Element in Sorted Array - two lower-bound searches.
 * LeetCode #34 (Medium)
 *
 * Time: O(log n), Space: O(1)
 */
public class Ques3_FindFirstAndLastPosition {

    public static int[] searchRange(int[] nums, int target) {
        int first = lowerBound(nums, target);
        if (first == nums.length || nums[first] != target) return new int[]{-1, -1};
        int last = lowerBound(nums, (long) target + 1) - 1;   // long avoids target+1 overflow
        return new int[]{first, last};
    }

    /**
     * Returns the first index i in [0, nums.length] with nums[i] >= key.
     * key is a long so callers can safely pass target + 1 without int overflow.
     */
    static int lowerBound(int[] nums, long key) {
        int lo = 0, hi = nums.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] >= key) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }

    static void main() {
        System.out.println(Arrays.toString(searchRange(new int[]{5, 7, 7, 8, 8, 10}, 8))); // [3, 4]
        System.out.println(Arrays.toString(searchRange(new int[]{5, 7, 7, 8, 8, 10}, 6))); // [-1, -1]
        System.out.println(Arrays.toString(searchRange(new int[]{}, 0)));                  // [-1, -1]
        System.out.println(Arrays.toString(searchRange(new int[]{2, 2, 2, 2}, 2)));        // [0, 3]
    }
}
