package ra.hul.dsa.fastslowpointers;

/**
 * Find the Duplicate Number - Treat the array as an implicit linked list (i -> nums[i]) and find the cycle entry.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques3_FindTheDuplicateNumber {

    public static int findDuplicate(int[] nums) {
        // Phase 1: find a meeting point inside the cycle.
        int slow = nums[0];
        int fast = nums[nums[0]];
        while (slow != fast) {
            slow = nums[slow];        // +1
            fast = nums[nums[fast]];  // +2
        }
        // Phase 2: restart slow at the "head" (index 0); both advance +1 to the entry.
        slow = 0;
        while (slow != fast) {
            slow = nums[slow];
            fast = nums[fast];
        }
        return slow; // the duplicated value (cycle entry)
    }

    static void main() {
        System.out.println(findDuplicate(new int[]{1, 3, 4, 2, 2}));    // 2
        System.out.println(findDuplicate(new int[]{3, 1, 3, 4, 2}));    // 3
        System.out.println(findDuplicate(new int[]{1, 1}));            // 1
        System.out.println(findDuplicate(new int[]{2, 2, 2, 2, 2}));    // 2
        System.out.println(findDuplicate(new int[]{1, 3, 2, 4, 2}));    // 2
    }
}
