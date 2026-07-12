package ra.hul.dsa.tree;

/**
 * Lowest Common Ancestor of a Binary Tree - LCA in a general binary tree (no BST ordering).
 * LeetCode #236 (Medium)
 *
 * Recurse: if the current node is p or q, it is a candidate. Search both subtrees; if p and q are
 * found in different subtrees, the current node is the LCA. Otherwise bubble up whichever side found
 * a target.
 *
 * Time: O(n), Space: O(h)
 */
public class Ques8_LowestCommonAncestorBinaryTree {

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

    public static TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if (left != null && right != null) return root; // p and q split here
        return left != null ? left : right;
    }

    static void main() {
        //        3
        //       / \
        //      5   1
        //     / \ / \
        //    6  2 0  8
        //      / \
        //     7   4
        TreeNode n7 = new TreeNode(7), n4 = new TreeNode(4);
        TreeNode n2 = new TreeNode(2, n7, n4);
        TreeNode n6 = new TreeNode(6);
        TreeNode n5 = new TreeNode(5, n6, n2);
        TreeNode n0 = new TreeNode(0), n8 = new TreeNode(8);
        TreeNode n1 = new TreeNode(1, n0, n8);
        TreeNode root = new TreeNode(3, n5, n1);

        boolean ok = true;

        TreeNode a = lowestCommonAncestor(root, n5, n1);
        System.out.println("LCA(5,1)=" + a.val + " expected 3");
        ok &= a.val == 3;

        TreeNode b = lowestCommonAncestor(root, n5, n4);
        System.out.println("LCA(5,4)=" + b.val + " expected 5");
        ok &= b.val == 5;

        TreeNode c = lowestCommonAncestor(root, n6, n4);
        System.out.println("LCA(6,4)=" + c.val + " expected 5");
        ok &= c.val == 5;

        TreeNode d = lowestCommonAncestor(root, n7, n8);
        System.out.println("LCA(7,8)=" + d.val + " expected 3");
        ok &= d.val == 3;

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
