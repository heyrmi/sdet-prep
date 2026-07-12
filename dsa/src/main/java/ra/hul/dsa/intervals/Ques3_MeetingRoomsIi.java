package ra.hul.dsa.intervals;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Meeting Rooms II - minimum number of conference rooms required (peak concurrency).
 *
 * Time: O(n log n), Space: O(n) - min-heap of end times
 */
public class Ques3_MeetingRoomsIi {

    public static int minMeetingRooms(int[][] intervals) {
        if (intervals.length == 0) return 0;

        // Sort by start. comparingInt is overflow-safe.
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        // Min-heap of end times: top = the room that frees up soonest.
        PriorityQueue<Integer> ends = new PriorityQueue<>();
        for (int[] cur : intervals) {
            // <= : a room freeing exactly at this start can be reused.
            if (!ends.isEmpty() && ends.peek() <= cur[0]) {
                ends.poll();
            }
            ends.add(cur[1]);
        }
        return ends.size();
    }

    static void main() {
        System.out.println(minMeetingRooms(new int[][]{{0, 30}, {5, 10}, {15, 20}})); // 2
        System.out.println(minMeetingRooms(new int[][]{{7, 10}, {2, 4}}));            // 1
        System.out.println(minMeetingRooms(new int[][]{{1, 5}, {5, 10}}));            // 1
    }
}
