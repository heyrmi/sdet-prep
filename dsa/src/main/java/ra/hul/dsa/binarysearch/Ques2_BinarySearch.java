package ra.hul.dsa.binarysearch;

/**
 * Binary Search - return the index of target in a sorted distinct array, or -1.
 * LeetCode #704 (Easy)
 *
 * Time: O(log n), Space: O(1)
 */
public class Ques2_BinarySearch {

    public static int search(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;     // inclusive on both ends
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;     // overflow-safe midpoint
            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }

    static void main() {
        System.out.println(search(new int[]{-1, 0, 3, 5, 9, 12}, 9)); // 4
        System.out.println(search(new int[]{-1, 0, 3, 5, 9, 12}, 2)); // -1
        System.out.println(search(new int[]{5}, 5));                  // 0
    }
}
