package ra.hul.dsa.monotonicstack;

import java.util.Arrays;

/**
 * Car Fleet - Count fleets of cars arriving at a target; a faster car catching a slower one merges.
 * LeetCode #853 (Medium)
 *
 * Sort cars by starting position descending (closest to target first). For each car compute the
 * time to reach the target. Walking from the front car, a car forms a new fleet only if its arrival
 * time is strictly greater than the current fleet's lead time; otherwise it catches up and merges.
 * Times behave like a monotonic stack of fleet-lead arrival times.
 *
 * Time: O(n log n), Space: O(n)
 */
public class Ques4_CarFleet {

    public static int carFleet(int target, int[] position, int[] speed) {
        int n = position.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        // sort by position descending: closest to target processed first
        Arrays.sort(idx, (a, b) -> position[b] - position[a]);

        int fleets = 0;
        double leadTime = 0.0; // arrival time of the current fleet's lead car
        for (int i = 0; i < n; i++) {
            int j = idx[i];
            double time = (double) (target - position[j]) / speed[j];
            if (time > leadTime) { // cannot catch the car ahead -> new fleet
                fleets++;
                leadTime = time;
            }
            // else it merges into the fleet ahead (arrives no later)
        }
        return fleets;
    }

    static void main() {
        boolean ok = true;

        int r1 = carFleet(12, new int[]{10, 8, 0, 5, 3}, new int[]{2, 4, 1, 1, 3});
        System.out.println("fleets=" + r1 + " expected 3");
        ok &= r1 == 3;

        int r2 = carFleet(10, new int[]{3}, new int[]{3});
        System.out.println("fleets=" + r2 + " expected 1");
        ok &= r2 == 1;

        int r3 = carFleet(100, new int[]{0, 2, 4}, new int[]{4, 2, 1});
        System.out.println("fleets=" + r3 + " expected 1");
        ok &= r3 == 1;

        int r4 = carFleet(10, new int[]{}, new int[]{});
        System.out.println("fleets=" + r4 + " expected 0");
        ok &= r4 == 0;

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
