package ra.hul.dsa.heap;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Find K Pairs with Smallest Sums - K-way merge over sorted rows of the implicit sum matrix.
 * LeetCode #373 (Medium)
 *
 * Time: O(k log k), Space: O(k)
 */
public class Ques10_FindKPairsWithSmallestSums {

    public static List<List<Integer>> kSmallestPairs(int[] nums1, int[] nums2, int k) {
        List<List<Integer>> result = new ArrayList<>();
        if (nums1.length == 0 || nums2.length == 0 || k <= 0) return result;

        // Heap entry {i, j}: pair (nums1[i], nums2[j]); ordered by the sum.
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) ->
                Integer.compare(nums1[a[0]] + nums2[a[1]], nums1[b[0]] + nums2[b[1]]));

        // Seed the first column of the first min(k, n) rows - never need more rows than k pairs.
        int rows = Math.min(k, nums1.length);
        for (int i = 0; i < rows; i++) heap.offer(new int[]{i, 0});

        while (result.size() < k && !heap.isEmpty()) {
            int[] top = heap.poll();
            int i = top[0], j = top[1];
            result.add(List.of(nums1[i], nums2[j]));
            if (j + 1 < nums2.length) heap.offer(new int[]{i, j + 1}); // successor in this row
        }
        return result;
    }

    static void main() {
        System.out.println(kSmallestPairs(new int[]{1, 7, 11}, new int[]{2, 4, 6}, 3)); // [[1, 2], [1, 4], [1, 6]]
        System.out.println(kSmallestPairs(new int[]{1, 1, 2}, new int[]{1, 2, 3}, 2));  // [[1, 1], [1, 1]]
        System.out.println(kSmallestPairs(new int[]{1, 2}, new int[]{3}, 3));           // [[1, 3], [2, 3]]
    }
}
