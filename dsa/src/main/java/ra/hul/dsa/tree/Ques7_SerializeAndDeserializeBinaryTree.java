package ra.hul.dsa.tree;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Serialize and Deserialize Binary Tree - Encode a tree to a string and rebuild it.
 * LeetCode #297 (Hard)
 *
 * Uses BFS level-order with "#" markers for null children, comma-separated. Deserialize replays
 * the queue: each non-null node pulls its two children from the token stream.
 *
 * Time: O(n) both ways, Space: O(n)
 */
public class Ques7_SerializeAndDeserializeBinaryTree {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    public static String serialize(TreeNode root) {
        if (root == null) return "#";
        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> q = new LinkedList<>(); // LinkedList allows null children in the queue
        q.offer(root);
        while (!q.isEmpty()) {
            TreeNode node = q.poll();
            if (node == null) {
                sb.append("#,");
                continue;
            }
            sb.append(node.val).append(',');
            q.offer(node.left);
            q.offer(node.right);
        }
        return sb.toString();
    }

    public static TreeNode deserialize(String data) {
        if (data.equals("#")) return null;
        String[] tokens = data.split(",");
        TreeNode root = new TreeNode(Integer.parseInt(tokens[0]));
        Queue<TreeNode> q = new ArrayDeque<>();
        q.offer(root);
        int i = 1;
        while (!q.isEmpty()) {
            TreeNode node = q.poll();
            if (!tokens[i].equals("#")) {
                node.left = new TreeNode(Integer.parseInt(tokens[i]));
                q.offer(node.left);
            }
            i++;
            if (!tokens[i].equals("#")) {
                node.right = new TreeNode(Integer.parseInt(tokens[i]));
                q.offer(node.right);
            }
            i++;
        }
        return root;
    }

    // canonical pre-order signature to compare two trees for structural equality
    private static String preorder(TreeNode root) {
        if (root == null) return "#";
        return root.val + "(" + preorder(root.left) + "," + preorder(root.right) + ")";
    }

    static void main() {
        //     1
        //    / \
        //   2   3
        //      / \
        //     4   5
        TreeNode root = new TreeNode(1,
                new TreeNode(2),
                new TreeNode(3, new TreeNode(4), new TreeNode(5)));

        String data = serialize(root);
        TreeNode rebuilt = deserialize(data);
        System.out.println("serialized: " + data);
        System.out.println("roundtrip equal: " + preorder(root).equals(preorder(rebuilt)));

        boolean ok = preorder(root).equals(preorder(rebuilt));

        // empty tree
        String emptyData = serialize(null);
        TreeNode emptyBack = deserialize(emptyData);
        ok &= emptyBack == null;
        System.out.println("empty roundtrip null: " + (emptyBack == null));

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
