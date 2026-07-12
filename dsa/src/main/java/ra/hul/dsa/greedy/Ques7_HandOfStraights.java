package ra.hul.dsa.greedy;

import java.util.TreeMap;

/**
 * Hand of Straights - can the hand be split into groups of groupSize consecutive values.
 *
 * Time: O(n log n), Space: O(n) - TreeMap of value -> count
 */
public class Ques7_HandOfStraights {

    public static boolean isNStraightHand(int[] hand, int groupSize) {
        if (hand.length % groupSize != 0) return false;

        TreeMap<Integer, Integer> count = new TreeMap<>();
        for (int c : hand) count.merge(c, 1, Integer::sum);

        while (!count.isEmpty()) {
            int start = count.firstKey();            // smallest card must start a run
            for (int v = start; v < start + groupSize; v++) {
                Integer have = count.get(v);
                if (have == null) return false;      // a needed consecutive value is missing
                if (have == 1) count.remove(v);       // last copy -> drop the key
                else count.put(v, have - 1);
            }
        }
        return true;
    }

    static void main() {
        System.out.println(isNStraightHand(new int[]{1, 2, 3, 6, 2, 3, 4, 7, 8}, 3)); // true
        System.out.println(isNStraightHand(new int[]{1, 2, 3, 4, 5}, 4));             // false
        System.out.println(isNStraightHand(new int[]{8, 10, 12}, 3));                 // false
    }
}
