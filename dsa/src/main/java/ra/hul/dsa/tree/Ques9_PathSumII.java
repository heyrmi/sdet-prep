package ra.hul.dsa.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Path Sum II - All root-to-leaf paths whose node values sum to a target.
 * LeetCode #113 (Medium)
 *
 * DFS backtracking: add the node to the current path, subtract from the remaining target, and at a
 * leaf record the path when the remainder hits zero. Remove the node on the way back up.
 *
 * Time: O(n^2) worst case (copying paths), Space: O(h) recursion
 */
public class Ques9_PathSumII {

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

    public static List<List<Integer>> pathSum(TreeNode root, int targetSum) {
        List<List<Integer>> result = new ArrayList<>();
        dfs(root, targetSum, new ArrayList<>(), result);
        return result;
    }

    private static void dfs(TreeNode node, int remaining, List<Integer> path, List<List<Integer>> result) {
        if (node == null) return;
        path.add(node.val);
        remaining -= node.val;
        if (node.left == null && node.right == null && remaining == 0) {
            result.add(new ArrayList<>(path)); // copy the current root-to-leaf path
        } else {
            dfs(node.left, remaining, path, result);
            dfs(node.right, remaining, path, result);
        }
        path.remove(path.size() - 1); // backtrack
    }

    static void main() {
        //         5
        //        / \
        //       4   8
        //      /   / \
        //     11  13  4
        //    /  \    / \
        //   7    2  5   1
        TreeNode root = new TreeNode(5,
                new TreeNode(4, new TreeNode(11, new TreeNode(7), new TreeNode(2)), null),
                new TreeNode(8, new TreeNode(13),
                        new TreeNode(4, new TreeNode(5), new TreeNode(1))));

        List<List<Integer>> res = pathSum(root, 22);
        System.out.println(res); // [[5, 4, 11, 2], [5, 8, 4, 5]]
        boolean ok = res.size() == 2
                && res.contains(List.of(5, 4, 11, 2))
                && res.contains(List.of(5, 8, 4, 5));

        List<List<Integer>> none = pathSum(new TreeNode(1, new TreeNode(2), new TreeNode(3)), 5);
        System.out.println(none + " expected []");
        ok &= none.isEmpty();

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
