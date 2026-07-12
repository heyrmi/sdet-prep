package ra.hul.dsa.tree;

/**
 * Diameter of Binary Tree - Length (in edges) of the longest path between any two nodes.
 * LeetCode #543 (Easy)
 *
 * Post-order DFS returns each subtree's height. At every node the longest path passing through it is
 * leftHeight + rightHeight edges; track the global max while heights bubble up.
 *
 * Time: O(n), Space: O(h)
 */
public class Ques10_DiameterOfBinaryTree {

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

    private static int best;

    public static int diameterOfBinaryTree(TreeNode root) {
        best = 0;
        height(root);
        return best;
    }

    private static int height(TreeNode node) {
        if (node == null) return 0;
        int left = height(node.left);
        int right = height(node.right);
        best = Math.max(best, left + right); // path through this node, in edges
        return 1 + Math.max(left, right);
    }

    static void main() {
        //     1
        //    / \
        //   2   3
        //  / \
        // 4   5
        TreeNode root = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3));

        boolean ok = true;
        int d1 = diameterOfBinaryTree(root);
        System.out.println("diameter=" + d1 + " expected 3"); // 4-2-1-3
        ok &= d1 == 3;

        int d2 = diameterOfBinaryTree(new TreeNode(1, new TreeNode(2), null));
        System.out.println("diameter=" + d2 + " expected 1");
        ok &= d2 == 1;

        int d3 = diameterOfBinaryTree(new TreeNode(1));
        System.out.println("diameter=" + d3 + " expected 0");
        ok &= d3 == 0;

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
