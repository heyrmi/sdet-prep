package ra.hul.dsa.intervals;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Meeting Rooms - can a person attend all meetings (no two overlap)?
 *
 * Time: O(n log n), Space: O(1)
 */
public class Ques2_MeetingRooms {

    public static boolean canAttendMeetings(int[][] intervals) {
        // Sort by start. comparingInt is overflow-safe.
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        for (int i = 1; i < intervals.length; i++) {
            // Strict <: a meeting starting exactly when the previous ends is fine (back-to-back).
            if (intervals[i][0] < intervals[i - 1][1]) {
                return false;
            }
        }
        return true;
    }

    static void main() {
        System.out.println(canAttendMeetings(new int[][]{{0, 30}, {5, 10}, {15, 20}})); // false
        System.out.println(canAttendMeetings(new int[][]{{7, 10}, {2, 4}}));            // true
        System.out.println(canAttendMeetings(new int[][]{{1, 5}, {5, 10}}));            // true
    }
}
