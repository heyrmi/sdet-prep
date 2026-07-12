package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Course Schedule II - return a valid ordering of courses (topological sort via Kahn's algorithm).
 * LeetCode #210 (Medium)
 *
 * Time: O(V + E), Space: O(V + E)
 */
public class Ques8_CourseScheduleII {

    public static int[] findOrder(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[numCourses];

        for (int[] p : prerequisites) {
            int a = p[0], b = p[1];     // take b before a => edge b -> a
            adj.get(b).add(a);
            indegree[a]++;
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < numCourses; i++) {
            if (indegree[i] == 0) queue.add(i);
        }

        int[] order = new int[numCourses];
        int idx = 0;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order[idx++] = u;
            for (int v : adj.get(u)) {
                if (--indegree[v] == 0) queue.add(v);
            }
        }

        return idx == numCourses ? order : new int[0];   // fewer placed => cycle
    }

    static void main() {
        System.out.println(Arrays.toString(findOrder(2, new int[][]{{1, 0}})));                          // [0, 1]
        System.out.println(Arrays.toString(findOrder(4, new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}}))); // [0, 1, 2, 3]
        System.out.println(Arrays.toString(findOrder(2, new int[][]{{0, 1}, {1, 0}})));                  // []
    }
}
