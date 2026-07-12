package ra.hul.dsa.prefixsum;

import java.util.HashMap;
import java.util.Map;

/**
 * Contiguous Array - longest subarray with equal numbers of 0s and 1s, via 0->-1 encoding + prefix sums.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques3_ContiguousArray {

    public static int findMaxLength(int[] nums) {
        Map<Integer, Integer> firstIndex = new HashMap<>();
        firstIndex.put(0, -1); // empty prefix sits just before index 0
        int cur = 0, best = 0;
        for (int i = 0; i < nums.length; i++) {
            cur += (nums[i] == 1) ? 1 : -1; // encode 0 -> -1
            if (firstIndex.containsKey(cur)) {
                best = Math.max(best, i - firstIndex.get(cur));
            } else {
                firstIndex.put(cur, i); // keep only the earliest occurrence
            }
        }
        return best;
    }

    static void main() {
        System.out.println(findMaxLength(new int[]{0, 1}));                      // 2
        System.out.println(findMaxLength(new int[]{0, 1, 0}));                   // 2
        System.out.println(findMaxLength(new int[]{0, 1, 1, 0, 1, 1, 1, 0}));    // 4
    }
}
