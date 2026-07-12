package ra.hul.dsa.tree;

import java.util.HashMap;
import java.util.Map;

/**
 * Construct Binary Tree from Preorder and Inorder Traversal - Rebuild the unique tree.
 * LeetCode #105 (Medium)
 *
 * Preorder gives the root first; its position in inorder splits left/right subtrees. Consume preorder
 * left-to-right with a moving index and recurse over inorder bounds, using a value->index map for O(1)
 * splits (all values assumed distinct).
 *
 * Time: O(n), Space: O(n)
 */
public class Ques12_ConstructBinaryTreeFromPreorderAndInorder {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    private static int preIdx;
    private static Map<Integer, Integer> inIndex;

    public static TreeNode buildTree(int[] preorder, int[] inorder) {
        preIdx = 0;
        inIndex = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) inIndex.put(inorder[i], i);
        return build(preorder, 0, inorder.length - 1);
    }

    private static TreeNode build(int[] preorder, int inLeft, int inRight) {
        if (inLeft > inRight) return null;
        int rootVal = preorder[preIdx++];
        TreeNode root = new TreeNode(rootVal);
        int mid = inIndex.get(rootVal);
        root.left = build(preorder, inLeft, mid - 1);   // must build left before right (preorder order)
        root.right = build(preorder, mid + 1, inRight);
        return root;
    }

    // level-order flatten (with nulls trimmed) for verification
    private static String preorderStr(TreeNode root) {
        if (root == null) return "#";
        return root.val + "(" + preorderStr(root.left) + "," + preorderStr(root.right) + ")";
    }

    static void main() {
        int[] preorder = {3, 9, 20, 15, 7};
        int[] inorder = {9, 3, 15, 20, 7};
        TreeNode root = buildTree(preorder, inorder);
        //   3
        //  / \
        // 9  20
        //   /  \
        //  15   7
        String expected = "3(9(#,#),20(15(#,#),7(#,#)))";
        String got = preorderStr(root);
        System.out.println("structure: " + got);
        System.out.println("expected : " + expected);

        boolean ok = got.equals(expected);

        int[] p2 = {-1};
        int[] i2 = {-1};
        TreeNode single = buildTree(p2, i2);
        ok &= single.val == -1 && single.left == null && single.right == null;
        System.out.println("single node ok: " + (single.val == -1));

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
