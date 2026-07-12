package ra.hul.dsa.greedy;

/**
 * Merge Triplets to Form Target Triplet - can element-wise-max merges produce the target triplet.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques6_MergeTripletsToFormTargetTriplet {

    public static boolean mergeTriplets(int[][] triplets, int[] target) {
        boolean can0 = false, can1 = false, can2 = false;
        for (int[] t : triplets) {
            // A triplet that overshoots any coordinate can never be merged in.
            if (t[0] > target[0] || t[1] > target[1] || t[2] > target[2]) continue;
            if (t[0] == target[0]) can0 = true;
            if (t[1] == target[1]) can1 = true;
            if (t[2] == target[2]) can2 = true;
        }
        return can0 && can1 && can2;
    }

    static void main() {
        System.out.println(mergeTriplets(new int[][]{{2, 5, 3}, {1, 8, 4}, {1, 7, 5}}, new int[]{2, 7, 5}));
        // true
        System.out.println(mergeTriplets(new int[][]{{3, 4, 5}, {4, 5, 6}}, new int[]{3, 2, 5}));
        // false
        System.out.println(mergeTriplets(new int[][]{{2, 5, 3}, {2, 3, 4}, {1, 2, 5}, {5, 2, 3}}, new int[]{5, 5, 5}));
        // true
    }
}
