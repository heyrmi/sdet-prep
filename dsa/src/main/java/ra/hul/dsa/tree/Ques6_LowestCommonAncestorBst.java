package ra.hul.dsa.tree;

/**
 * Lowest Common Ancestor of a BST - walk down until p and q split; that node is the LCA.
 * LeetCode #235 (Medium)
 *
 * Time: O(h), Space: O(1)
 */
public class Ques6_LowestCommonAncestorBst {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    static int lowestCommonAncestor(TreeNode root, int p, int q) {
        TreeNode node = root;
        while (node != null) {
            if (p < node.val && q < node.val) {
                node = node.left;            // both smaller -> LCA is left
            } else if (p > node.val && q > node.val) {
                node = node.right;           // both larger -> LCA is right
            } else {
                return node.val;             // they split here (or node is p/q) -> this is the LCA
            }
        }
        return root.val; // unreachable on valid input (both p,q guaranteed present)
    }

    static void main() {
        //            6
        //          /   \
        //         2     8
        //        / \   / \
        //       0   4 7   9
        //          / \
        //         3   5
        TreeNode root = new TreeNode(6);
        root.left = new TreeNode(2);
        root.right = new TreeNode(8);
        root.left.left = new TreeNode(0);
        root.left.right = new TreeNode(4);
        root.right.left = new TreeNode(7);
        root.right.right = new TreeNode(9);
        root.left.right.left = new TreeNode(3);
        root.left.right.right = new TreeNode(5);
        System.out.println(lowestCommonAncestor(root, 2, 8)); // 6
        System.out.println(lowestCommonAncestor(root, 2, 4)); // 2
        System.out.println(lowestCommonAncestor(root, 3, 5)); // 4
    }
}
