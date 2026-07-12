package ra.hul.dsa.tree;

/**
 * Binary Tree Maximum Path Sum - Max sum of any path (need not pass through the root).
 * LeetCode #124 (Hard)
 *
 * Post-order DFS returns the best downward gain from a node (node value plus the larger non-negative
 * child gain). At each node the best path through it is node.val + leftGain + rightGain; track the
 * global max. Negative child gains are clamped to 0 (skip that branch).
 *
 * Time: O(n), Space: O(h)
 */
public class Ques11_BinaryTreeMaximumPathSum {

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

    public static int maxPathSum(TreeNode root) {
        best = Integer.MIN_VALUE;
        gain(root);
        return best;
    }

    private static int gain(TreeNode node) {
        if (node == null) return 0;
        int left = Math.max(0, gain(node.left));   // drop negative branches
        int right = Math.max(0, gain(node.right));
        best = Math.max(best, node.val + left + right); // path bending at this node
        return node.val + Math.max(left, right);        // best straight-down gain
    }

    static void main() {
        boolean ok = true;

        // [1,2,3] -> 2 + 1 + 3 = 6
        int r1 = maxPathSum(new TreeNode(1, new TreeNode(2), new TreeNode(3)));
        System.out.println("maxPathSum=" + r1 + " expected 6");
        ok &= r1 == 6;

        // [-10,9,20,null,null,15,7] -> 15 + 20 + 7 = 42
        TreeNode t2 = new TreeNode(-10,
                new TreeNode(9),
                new TreeNode(20, new TreeNode(15), new TreeNode(7)));
        int r2 = maxPathSum(t2);
        System.out.println("maxPathSum=" + r2 + " expected 42");
        ok &= r2 == 42;

        // single negative node -> -3
        int r3 = maxPathSum(new TreeNode(-3));
        System.out.println("maxPathSum=" + r3 + " expected -3");
        ok &= r3 == -3;

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
