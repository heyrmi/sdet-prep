package ra.hul.dsa.binarysearch;

/**
 * Search Insert Position - Find target index or where it would be inserted in sorted array.
 * LeetCode #35 (Easy)
 *
 * Time: O(log n), Space: O(1)
 */
public class Ques1_SearchInsertPosition {

    public static int searchInsert(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return left;
    }

    public static void main(String[] args) {
        System.out.println(searchInsert(new int[]{1, 3, 5, 6}, 5)); // 2
        System.out.println(searchInsert(new int[]{1, 3, 5, 6}, 2)); // 1
        System.out.println(searchInsert(new int[]{1, 3, 5, 6}, 7)); // 4
        System.out.println(searchInsert(new int[]{1, 3, 5, 6}, 0)); // 0
    }
}
