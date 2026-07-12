package ra.hul.dsa.prefixsum;

/**
 * Find Pivot Index - leftmost index where the sum to its left equals the sum to its right.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques4_FindPivotIndex {

    public static int pivotIndex(int[] nums) {
        long total = 0;
        for (int x : nums) total += x;

        long leftSum = 0;
        for (int i = 0; i < nums.length; i++) {
            long rightSum = total - leftSum - nums[i];
            if (leftSum == rightSum) return i; // leftmost pivot
            leftSum += nums[i];                // update AFTER checking index i
        }
        return -1;
    }

    static void main() {
        System.out.println(pivotIndex(new int[]{1, 7, 3, 6, 5, 6})); // 3
        System.out.println(pivotIndex(new int[]{1, 2, 3}));          // -1
        System.out.println(pivotIndex(new int[]{2, 1, -1}));         // 0
    }
}
