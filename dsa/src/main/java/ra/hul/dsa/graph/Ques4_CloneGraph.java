package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Clone Graph - deep-copy a connected undirected graph using an original->clone map.
 * LeetCode #133 (Medium)
 *
 * Time: O(V + E), Space: O(V)
 */
public class Ques4_CloneGraph {

    /** Graph node: a value and a list of neighbor references. */
    static class Node {
        int val;
        List<Node> neighbors;

        Node() {
            this.val = 0;
            this.neighbors = new ArrayList<>();
        }

        Node(int val) {
            this.val = val;
            this.neighbors = new ArrayList<>();
        }

        Node(int val, List<Node> neighbors) {
            this.val = val;
            this.neighbors = neighbors;
        }
    }

    public static Node cloneGraph(Node node) {
        if (node == null) return null;
        return dfs(node, new HashMap<>());
    }

    private static Node dfs(Node orig, Map<Node, Node> made) {
        Node existing = made.get(orig);
        if (existing != null) return existing;       // already cloned: reuse

        Node copy = new Node(orig.val);
        made.put(orig, copy);                         // register BEFORE recursing
        for (Node nbr : orig.neighbors) {
            copy.neighbors.add(dfs(nbr, made));
        }
        return copy;
    }

    /** Serialize a graph as {val=[sorted neighbor vals]} for readable, deterministic output. */
    private static Map<Integer, List<Integer>> serialize(Node start) {
        Map<Integer, List<Integer>> out = new TreeMap<>();
        if (start == null) return out;
        Map<Integer, Node> seen = new HashMap<>();
        Deque<Node> q = new ArrayDeque<>();
        q.add(start);
        seen.put(start.val, start);
        while (!q.isEmpty()) {
            Node n = q.poll();
            List<Integer> nbrVals = new ArrayList<>();
            for (Node nb : n.neighbors) {
                nbrVals.add(nb.val);
                if (!seen.containsKey(nb.val)) {
                    seen.put(nb.val, nb);
                    q.add(nb);
                }
            }
            nbrVals.sort(null);
            out.put(n.val, nbrVals);
        }
        return out;
    }

    static void main() {
        // Build the square graph: 1-2-3-4-1
        Node n1 = new Node(1), n2 = new Node(2), n3 = new Node(3), n4 = new Node(4);
        n1.neighbors.add(n2); n1.neighbors.add(n4);
        n2.neighbors.add(n1); n2.neighbors.add(n3);
        n3.neighbors.add(n2); n3.neighbors.add(n4);
        n4.neighbors.add(n1); n4.neighbors.add(n3);

        Node clone = cloneGraph(n1);
        System.out.println(clone != n1);                 // true  (deep copy, new object)
        System.out.println(serialize(clone));            // {1=[2, 4], 2=[1, 3], 3=[2, 4], 4=[1, 3]}

        Node single = cloneGraph(new Node(1));
        System.out.println(serialize(single));           // {1=[]}
        System.out.println(cloneGraph(null));            // null
    }
}
