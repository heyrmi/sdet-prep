package ra.hul.dsa.binarysearch;

/**
 * Median of Two Sorted Arrays - binary-search the partition point in the smaller array.
 * LeetCode #4 (Hard)
 *
 * Time: O(log(min(m, n))), Space: O(1)
 */
public class Ques8_MedianOfTwoSortedArrays {

    public static double findMedianSortedArrays(int[] nums1, int[] nums2) {
        if (nums1.length > nums2.length) {        // always search the smaller array
            int[] tmp = nums1; nums1 = nums2; nums2 = tmp;
        }
        int m = nums1.length, n = nums2.length;
        int half = (m + n + 1) / 2;               // size of the left half
        int lo = 0, hi = m;
        while (lo <= hi) {
            int i = lo + (hi - lo) / 2;           // elements taken from nums1
            int j = half - i;                     // elements taken from nums2

            int L1 = (i == 0) ? Integer.MIN_VALUE : nums1[i - 1];
            int R1 = (i == m) ? Integer.MAX_VALUE : nums1[i];
            int L2 = (j == 0) ? Integer.MIN_VALUE : nums2[j - 1];
            int R2 = (j == n) ? Integer.MAX_VALUE : nums2[j];

            if (L1 > R2) {
                hi = i - 1;                       // too many from nums1
            } else if (L2 > R1) {
                lo = i + 1;                       // too few from nums1
            } else {                              // correct partition
                int leftMax = Math.max(L1, L2);
                if (((m + n) & 1) == 1) return leftMax;            // odd total
                int rightMin = Math.min(R1, R2);
                return (leftMax + (long) rightMin) / 2.0;          // even total
            }
        }
        throw new IllegalArgumentException("inputs are not sorted arrays");
    }

    static void main() {
        System.out.println(findMedianSortedArrays(new int[]{1, 3}, new int[]{2}));       // 2.0
        System.out.println(findMedianSortedArrays(new int[]{1, 2}, new int[]{3, 4}));    // 2.5
        System.out.println(findMedianSortedArrays(new int[]{}, new int[]{1}));           // 1.0
        System.out.println(findMedianSortedArrays(new int[]{0, 0}, new int[]{0, 0}));    // 0.0
    }
}
