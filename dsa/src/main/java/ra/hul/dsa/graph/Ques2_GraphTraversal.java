package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Implement Graph Traversal - undirected graph via adjacency list with deterministic BFS/DFS.
 *
 * Time: O(V + E) per traversal, Space: O(V + E)
 */
public class Ques2_GraphTraversal {

    /** Undirected graph backed by an adjacency list. Neighbors visited in ascending order. */
    static class Graph {
        private final Map<Integer, List<Integer>> adj = new HashMap<>();

        /** Add an UNDIRECTED edge u<->v (both directions). */
        public void addEdge(int u, int v) {
            adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            adj.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        }

        /** Neighbors of u, sorted ascending; empty list if u is unknown/isolated. */
        private List<Integer> sortedNeighbors(int u) {
            List<Integer> ns = adj.getOrDefault(u, Collections.emptyList());
            List<Integer> copy = new ArrayList<>(ns);
            Collections.sort(copy);
            return copy;
        }

        public int[] bfs(int start) {
            List<Integer> order = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            Queue<Integer> q = new ArrayDeque<>();
            q.add(start);
            visited.add(start);                 // mark on enqueue
            while (!q.isEmpty()) {
                int u = q.poll();
                order.add(u);
                for (int v : sortedNeighbors(u)) {
                    if (visited.add(v)) {       // add() returns false if already present
                        q.add(v);
                    }
                }
            }
            return toIntArray(order);
        }

        public int[] dfs(int start) {
            List<Integer> order = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            dfsVisit(start, visited, order);
            return toIntArray(order);
        }

        private void dfsVisit(int u, Set<Integer> visited, List<Integer> order) {
            visited.add(u);
            order.add(u);
            for (int v : sortedNeighbors(u)) {
                if (!visited.contains(v)) {
                    dfsVisit(v, visited, order);
                }
            }
        }

        private static int[] toIntArray(List<Integer> list) {
            int[] a = new int[list.size()];
            for (int i = 0; i < a.length; i++) a[i] = list.get(i);
            return a;
        }
    }

    static void main() {
        Graph g = new Graph();
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(1, 3);
        g.addEdge(2, 3);
        System.out.println(Arrays.toString(g.bfs(0)));   // [0, 1, 2, 3]
        System.out.println(Arrays.toString(g.dfs(0)));   // [0, 1, 3, 2]
    }
}
