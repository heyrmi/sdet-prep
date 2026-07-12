package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Course Schedule - can all courses be finished given prerequisites (directed cycle detection).
 * LeetCode #207 (Medium)
 *
 * Time: O(V + E), Space: O(V + E)
 */
public class Ques5_CourseSchedule {

    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) adj.add(new ArrayList<>());
        int[] indeg = new int[numCourses];

        for (int[] p : prerequisites) {
            int a = p[0], b = p[1];   // must take b before a  =>  edge b -> a
            adj.get(b).add(a);
            indeg[a]++;
        }

        Queue<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < numCourses; i++) if (indeg[i] == 0) q.add(i);

        int processed = 0;
        while (!q.isEmpty()) {
            int u = q.poll();
            processed++;
            for (int next : adj.get(u)) {
                if (--indeg[next] == 0) q.add(next);
            }
        }
        return processed == numCourses;   // all processed => acyclic
    }

    static void main() {
        System.out.println(canFinish(2, new int[][]{{1, 0}}));                          // true
        System.out.println(canFinish(2, new int[][]{{1, 0}, {0, 1}}));                  // false
        System.out.println(canFinish(4, new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}}));  // true
    }
}
