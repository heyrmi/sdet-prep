package ra.hul.dsa.prefixsum;

import java.util.HashMap;
import java.util.Map;

/**
 * Continuous Subarray Sum - does a subarray of length >= 2 exist whose sum is a multiple of k?
 *
 * Time: O(n), Space: O(min(n, k))
 */
public class Ques6_ContinuousSubarraySum {

    public static boolean checkSubarraySum(int[] nums, int k) {
        Map<Integer, Integer> firstIndex = new HashMap<>();
        firstIndex.put(0, -1); // empty prefix: remainder 0 at index -1
        long cur = 0;          // long: sum can reach ~1e14
        for (int i = 0; i < nums.length; i++) {
            cur += nums[i];
            int r = (int) (((cur % k) + k) % k); // normalize (k >= 1, but stays safe)
            Integer j = firstIndex.get(r);
            if (j != null) {
                if (i - j >= 2) return true; // subarray (j+1 .. i) has length >= 2
            } else {
                firstIndex.put(r, i);        // keep only the earliest occurrence
            }
        }
        return false;
    }

    static void main() {
        System.out.println(checkSubarraySum(new int[]{23, 2, 4, 6, 7}, 6));  // true
        System.out.println(checkSubarraySum(new int[]{23, 2, 6, 4, 7}, 6));  // true
        System.out.println(checkSubarraySum(new int[]{23, 2, 6, 4, 7}, 13)); // false
        System.out.println(checkSubarraySum(new int[]{0, 0}, 1));            // true
    }
}
