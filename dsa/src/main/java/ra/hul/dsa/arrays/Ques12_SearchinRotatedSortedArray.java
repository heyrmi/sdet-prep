package ra.hul.dsa.arrays;

public class Ques12_SearchinRotatedSortedArray {

    public static int search(int[] nums, int target) {
        int low = 0, high = nums.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) return mid;

            // Left half is sorted
            if (nums[low] <= nums[mid]) {
                if (nums[low] <= target && target < nums[mid]) {
                    high = mid - 1; // target is in the left sorted half
                } else {
                    low = mid + 1; // target is in the right half
                }
            }
            // Right half is sorted
            else {
                if (nums[mid] < target && target <= nums[high]) {
                    low = mid + 1; // target is in the sorted right half
                } else {
                    high = mid - 1; // target is in the left half
                }
            }
        }
        return -1; // if nothing is found
    }

    static void main() {
        System.out.println(search(new int[]{4, 5, 6, 7, 0, 1, 2}, 0));   // 4
        System.out.println(search(new int[]{4, 5, 6, 7, 0, 1, 2}, 3));   // -1 (not found)
        System.out.println(search(new int[]{1}, 0));                     // -1
        System.out.println(search(new int[]{1}, 1));                     // 0
        System.out.println(search(new int[]{3, 1}, 1));                  // 1
    }
}