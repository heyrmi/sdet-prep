package ra.hul.dsa.intervals;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Minimum Number of Arrows to Burst Balloons - fewest vertical arrows to burst all interval balloons.
 *
 * Time: O(n log n), Space: O(1) - greedy, sort by end, shoot at the end
 */
public class Ques4_MinimumNumberOfArrowsToBurstBalloons {

    public static int findMinArrowShots(int[][] points) {
        if (points.length == 0) return 0;

        // Sort by END. comparingInt is overflow-safe (a[1] - b[1] can overflow).
        Arrays.sort(points, Comparator.comparingInt(a -> a[1]));

        int arrows = 1;
        long arrowX = points[0][1];   // shoot at the first balloon's end; long guards extremes
        for (int i = 1; i < points.length; i++) {
            // A balloon starting after the current arrow needs a new arrow.
            if (points[i][0] > arrowX) {
                arrows++;
                arrowX = points[i][1];
            }
            // else: points[i][0] <= arrowX -> already burst by the current arrow.
        }
        return arrows;
    }

    static void main() {
        System.out.println(findMinArrowShots(new int[][]{{10, 16}, {2, 8}, {1, 6}, {7, 12}})); // 2
        System.out.println(findMinArrowShots(new int[][]{{1, 2}, {3, 4}, {5, 6}, {7, 8}}));     // 4
        System.out.println(findMinArrowShots(new int[][]{{1, 2}, {2, 3}, {3, 4}, {4, 5}}));     // 2
    }
}
