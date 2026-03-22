package ra.hul.dsa.arrays;

public class Ques11_FindMinimuminRotatedSortedArray {

    public static int findMin(int[] nums) {
        int low = 0, high = nums.length - 1;

        while (low < high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] > nums[high]) {
                // min is in the right half excluding mid
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return nums[low];
    }

    static void main() {
        System.out.println(findMin(new int[]{3, 4, 5, 1, 2}));       // 1
        System.out.println(findMin(new int[]{4, 5, 6, 7, 0, 1, 2})); // 0
        System.out.println(findMin(new int[]{11, 13, 15, 17}));      // 11 (not rotated)
        System.out.println(findMin(new int[]{2, 1}));                // 1
    }
}
