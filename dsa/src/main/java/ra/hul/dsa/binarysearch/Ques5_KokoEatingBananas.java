package ra.hul.dsa.binarysearch;

/**
 * Koko Eating Bananas - binary search on the answer (minimum feasible eating speed).
 * LeetCode #875 (Medium)
 *
 * Time: O(n log(max pile)), Space: O(1)
 */
public class Ques5_KokoEatingBananas {

    public static int minEatingSpeed(int[] piles, int h) {
        int lo = 1, hi = 1;
        for (int p : piles) hi = Math.max(hi, p);    // upper bound = max pile
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (feasible(piles, mid, h)) hi = mid;   // mid works; try smaller
            else lo = mid + 1;
        }
        return lo;
    }

    /**
     * True if eating every pile at speed k finishes within h hours.
     * Hours for a pile of size p is ceil(p / k). Sum accumulates in a long.
     */
    static boolean feasible(int[] piles, int k, int h) {
        long hours = 0;
        for (int p : piles) hours += (p + (long) k - 1) / k;   // ceil division
        return hours <= h;
    }

    static void main() {
        System.out.println(minEatingSpeed(new int[]{3, 6, 7, 11}, 8));      // 4
        System.out.println(minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 5)); // 30
        System.out.println(minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 6)); // 23
    }
}
