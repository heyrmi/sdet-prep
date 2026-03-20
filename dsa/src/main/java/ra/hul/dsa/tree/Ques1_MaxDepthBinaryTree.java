package ra.hul.dsa.tree;

/**
 * Maximum Depth of Binary Tree - Find the max depth (root to farthest leaf).
 * LeetCode #104 (Easy)
 *
 * Time: O(n), Space: O(h) where h = height of tree
 */
public class Ques1_MaxDepthBinaryTree {

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

    public static int maxDepth(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    public static void main(String[] args) {
        //     3
        //    / \
        //   9  20
        //     /  \
        //    15   7
        TreeNode root = new TreeNode(3,
                new TreeNode(9),
                new TreeNode(20, new TreeNode(15), new TreeNode(7))
        );
        System.out.println(maxDepth(root)); // 3
        System.out.println(maxDepth(null)); // 0
    }
}
