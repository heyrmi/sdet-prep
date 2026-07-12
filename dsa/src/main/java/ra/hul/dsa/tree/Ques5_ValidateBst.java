package ra.hul.dsa.tree;

/**
 * Validate Binary Search Tree - every node must lie strictly inside its ancestor-derived range.
 * LeetCode #98 (Medium)
 *
 * Time: O(n), Space: O(h)
 */
public class Ques5_ValidateBst {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    static boolean isValidBST(TreeNode root) {
        return valid(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    // node.val must lie strictly inside the open interval (low, high).
    // long bounds: no int value can equal Long.MIN/MAX_VALUE, so Integer.MIN/MAX nodes work
    // and there is no overflow.
    private static boolean valid(TreeNode node, long low, long high) {
        if (node == null) return true;                       // empty subtree is valid
        if (node.val <= low || node.val >= high) return false;
        return valid(node.left, low, node.val)               // tighten upper bound
            && valid(node.right, node.val, high);            // tighten lower bound
    }

    static void main() {
        //   2          valid
        //  / \
        // 1   3
        TreeNode a = new TreeNode(2);
        a.left = new TreeNode(1);
        a.right = new TreeNode(3);
        System.out.println(isValidBST(a)); // true

        //   5          invalid (3 sits in 5's right subtree but 3 < 5)
        //  / \
        // 1   4
        //    / \
        //   3   6
        TreeNode b = new TreeNode(5);
        b.left = new TreeNode(1);
        b.right = new TreeNode(4);
        b.right.left = new TreeNode(3);
        b.right.right = new TreeNode(6);
        System.out.println(isValidBST(b)); // false
    }
}
