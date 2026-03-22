package ra.hul.dsa.arrays;

import java.util.HashSet;
import java.util.Set;

public class Ques3_ContainsDuplicate {
    public static boolean hasDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();

        for (int num : nums) {
            if (seen.contains(num)) {
                return true;
            }
            seen.add(num);
        }
        return false;
    }

    static void main() {
        System.out.println(hasDuplicate(new int[]{1, 2, 3, 1}));     // true
        System.out.println(hasDuplicate(new int[]{1, 2, 3, 4}));     // false
        System.out.println(hasDuplicate(new int[]{1, 1, 1, 1}));     // true
        System.out.println(hasDuplicate(new int[]{}));                // false
    }
}
