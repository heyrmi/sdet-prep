package ra.hul.dsa.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Binary Tree Level Order Traversal - list the values level by level, left to right (BFS).
 * LeetCode #102 (Medium)
 *
 * Time: O(n), Space: O(w) where w is the max level width
 */
public class Ques4_LevelOrderTraversal {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        Queue<TreeNode> q = new ArrayDeque<>();
        if (root != null) q.offer(root);
        while (!q.isEmpty()) {
            int size = q.size();                       // freeze this level's count
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left != null) q.offer(n.left);   // never offer null into ArrayDeque
                if (n.right != null) q.offer(n.right);
            }
            result.add(level);
        }
        return result;
    }

    static void main() {
        //        3
        //       / \
        //      9   20
        //         /  \
        //        15   7
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);
        System.out.println(levelOrder(root)); // [[3], [9, 20], [15, 7]]
    }
}
