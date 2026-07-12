package ra.hul.dsa.hashmap;

import java.util.HashSet;
import java.util.Set;

/**
 * Longest Consecutive Sequence - length of the longest run of consecutive integers, in O(n).
 *
 * Time: O(n), Space: O(n) - HashSet for O(1) membership
 */
public class Ques4_LongestConsecutiveSequence {

    public static int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int x : nums) set.add(x); // also collapses duplicates

        int best = 0;
        for (int x : set) {
            // Only start counting at the beginning of a run.
            if (set.contains(x - 1)) continue;
            int length = 1;
            int next = x + 1;
            while (set.contains(next)) {
                length++;
                next++;
            }
            best = Math.max(best, length);
        }
        return best;
    }

    static void main() {
        System.out.println(longestConsecutive(new int[]{100, 4, 200, 1, 3, 2}));            // 4
        System.out.println(longestConsecutive(new int[]{0, 3, 7, 2, 5, 8, 4, 6, 0, 1}));    // 9
        System.out.println(longestConsecutive(new int[]{}));                                 // 0
        System.out.println(longestConsecutive(new int[]{5, 5, 5}));                          // 1
    }
}
