package ra.hul.dsa.prefixsum;

import java.util.HashMap;
import java.util.Map;

/**
 * Subarray Sum Equals K - count contiguous subarrays summing to exactly k using prefix sums + a HashMap.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques2_SubarraySumEqualsK {

    public static int subarraySum(int[] nums, int k) {
        Map<Long, Integer> seen = new HashMap<>();
        seen.put(0L, 1); // empty prefix — counts subarrays starting at index 0
        long cur = 0;
        int count = 0;
        for (int x : nums) {
            cur += x;
            // Earlier prefixes equal to (cur - k) each close a sum-k subarray ending here.
            count += seen.getOrDefault(cur - k, 0);
            seen.merge(cur, 1, Integer::sum);
        }
        return count;
    }

    static void main() {
        System.out.println(subarraySum(new int[]{1, 1, 1}, 2));  // 2
        System.out.println(subarraySum(new int[]{1, 2, 3}, 3));  // 2
        System.out.println(subarraySum(new int[]{1, -1, 0}, 0)); // 3
    }
}
