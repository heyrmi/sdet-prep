package ra.hul.dsa.graph;

import java.util.Arrays;

/**
 * Cheapest Flights Within K Stops - cheapest src->dst cost using at most k stops (hop-limited Bellman-Ford).
 * LeetCode #787 (Medium)
 *
 * Time: O(k*E), Space: O(V)
 */
public class Ques11_CheapestFlightsWithinKStops {

    public static int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        final int INF = Integer.MAX_VALUE;
        int[] dist = new int[n];
        Arrays.fill(dist, INF);
        dist[src] = 0;

        // k stops => up to k + 1 edges => k + 1 rounds.
        for (int round = 0; round <= k; round++) {
            int[] prev = Arrays.copyOf(dist, n);     // snapshot: relax from last round's values
            for (int[] f : flights) {
                int u = f[0], v = f[1], price = f[2];
                if (prev[u] == INF) continue;        // can't extend an unreached city (and avoids overflow)
                if (prev[u] + price < dist[v]) {
                    dist[v] = prev[u] + price;
                }
            }
        }

        return dist[dst] == INF ? -1 : dist[dst];
    }

    static void main() {
        int[][] f1 = {{0, 1, 100}, {1, 2, 100}, {2, 0, 100}, {1, 3, 600}, {2, 3, 200}};
        System.out.println(findCheapestPrice(4, f1, 0, 3, 1));   // 700

        int[][] f2 = {{0, 1, 100}, {1, 2, 100}, {0, 2, 500}};
        System.out.println(findCheapestPrice(3, f2, 0, 2, 1));   // 200
        System.out.println(findCheapestPrice(3, f2, 0, 2, 0));   // 500
    }
}
