package ra.hul.dsa.tree;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Invert Binary Tree - mirror the tree left-to-right by swapping every node's children.
 * LeetCode #226 (Easy)
 *
 * Time: O(n), Space: O(h)
 */
public class Ques3_InvertBinaryTree {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    static TreeNode invertTree(TreeNode root) {
        if (root == null) return null;
        TreeNode temp = root.left;       // save before overwriting
        root.left = root.right;
        root.right = temp;
        invertTree(root.left);
        invertTree(root.right);
        return root;
    }

    // Level-order values, used only to make the inverted tree printable.
    static List<Integer> levelOrderVals(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Queue<TreeNode> q = new ArrayDeque<>();
        if (root != null) q.offer(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            out.add(n.val);
            if (n.left != null) q.offer(n.left);
            if (n.right != null) q.offer(n.right);
        }
        return out;
    }

    static void main() {
        //        4                 4
        //      /   \             /   \
        //     2     7    ->     7     2
        //    / \   / \         / \   / \
        //   1   3 6   9       9   6 3   1
        TreeNode root = new TreeNode(4);
        root.left = new TreeNode(2);
        root.right = new TreeNode(7);
        root.left.left = new TreeNode(1);
        root.left.right = new TreeNode(3);
        root.right.left = new TreeNode(6);
        root.right.right = new TreeNode(9);
        invertTree(root);
        System.out.println(levelOrderVals(root)); // [4, 7, 2, 9, 6, 3, 1]
    }
}
