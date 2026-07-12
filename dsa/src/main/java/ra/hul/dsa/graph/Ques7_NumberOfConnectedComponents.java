package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Number of Connected Components - count components of an undirected graph via BFS.
 * LeetCode #323 (Medium)
 *
 * Time: O(V + E), Space: O(V + E)
 */
public class Ques7_NumberOfConnectedComponents {

    public static int countComponents(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);    // undirected: both directions
            adj.get(e[1]).add(e[0]);
        }

        boolean[] visited = new boolean[n];
        int components = 0;
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                components++;
                bfs(i, adj, visited);
            }
        }
        return components;
    }

    private static void bfs(int start, List<List<Integer>> adj, boolean[] visited) {
        Deque<Integer> q = new ArrayDeque<>();
        q.add(start);
        visited[start] = true;          // mark on enqueue
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adj.get(u)) {
                if (!visited[v]) {
                    visited[v] = true;
                    q.add(v);
                }
            }
        }
    }

    static void main() {
        System.out.println(countComponents(5, new int[][]{{0, 1}, {1, 2}, {3, 4}}));            // 2
        System.out.println(countComponents(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}}));    // 1
        System.out.println(countComponents(4, new int[][]{}));                                  // 4
    }
}
