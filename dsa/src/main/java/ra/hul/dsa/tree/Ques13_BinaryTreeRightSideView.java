package ra.hul.dsa.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Binary Tree Right Side View - Values visible when the tree is viewed from the right.
 * LeetCode #199 (Medium)
 *
 * BFS level order: the last node dequeued at each level is the rightmost visible node.
 *
 * Time: O(n), Space: O(n)
 */
public class Ques13_BinaryTreeRightSideView {

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

    public static List<Integer> rightSideView(TreeNode root) {
        List<Integer> view = new ArrayList<>();
        if (root == null) return view;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.offer(root);
        while (!q.isEmpty()) {
            int levelSize = q.size();
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = q.poll();
                if (i == levelSize - 1) view.add(node.val); // rightmost in this level
                if (node.left != null) q.offer(node.left);
                if (node.right != null) q.offer(node.right);
            }
        }
        return view;
    }

    static void main() {
        //   1
        //    \
        //     3   (also 2 on left, but 2's right child 5 shows)
        //    /
        //   ...
        //     1
        //    / \
        //   2   3
        //    \   \
        //     5   4
        TreeNode root = new TreeNode(1,
                new TreeNode(2, null, new TreeNode(5)),
                new TreeNode(3, null, new TreeNode(4)));

        boolean ok = true;
        List<Integer> v1 = rightSideView(root);
        System.out.println(v1 + " expected [1, 3, 4]");
        ok &= v1.equals(List.of(1, 3, 4));

        List<Integer> v2 = rightSideView(null);
        System.out.println(v2 + " expected []");
        ok &= v2.isEmpty();

        List<Integer> v3 = rightSideView(new TreeNode(1, new TreeNode(2), null));
        System.out.println(v3 + " expected [1, 2]");
        ok &= v3.equals(List.of(1, 2));

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
