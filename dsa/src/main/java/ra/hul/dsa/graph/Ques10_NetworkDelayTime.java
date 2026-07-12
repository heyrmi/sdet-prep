package ra.hul.dsa.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Network Delay Time - min time for a signal from k to reach all nodes (Dijkstra).
 * LeetCode #743 (Medium)
 *
 * Time: O((V + E) log V), Space: O(V + E)
 */
public class Ques10_NetworkDelayTime {

    public static int networkDelayTime(int[][] times, int n, int k) {
        final int INF = Integer.MAX_VALUE;

        // Weighted adjacency list, 1-indexed (index 0 unused).
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
        for (int[] t : times) {
            adj.get(t[0]).add(new int[]{t[1], t[2]});   // u -> v, weight w
        }

        int[] dist = new int[n + 1];
        Arrays.fill(dist, INF);
        dist[k] = 0;

        // Min-heap of {dist, node}; compare by dist with Integer.compare (no overflow).
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        pq.add(new int[]{0, k});

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int d = top[0], u = top[1];
            if (d > dist[u]) continue;                  // stale entry
            for (int[] e : adj.get(u)) {
                int v = e[0], w = e[1];
                if (dist[u] != INF && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    pq.add(new int[]{dist[v], v});
                }
            }
        }

        int ans = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == INF) return -1;              // unreachable
            ans = Math.max(ans, dist[i]);
        }
        return ans;
    }

    static void main() {
        System.out.println(networkDelayTime(new int[][]{{2, 1, 1}, {2, 3, 1}, {3, 4, 1}}, 4, 2)); // 2
        System.out.println(networkDelayTime(new int[][]{{1, 2, 1}}, 2, 1));                        // 1
        System.out.println(networkDelayTime(new int[][]{{1, 2, 1}}, 2, 2));                        // -1
    }
}
