package ra.hul.dsa.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implement a Binary Search Tree - insert / contains / delete / inorder over ints (no duplicates).
 *
 * Time: O(h) per op (O(log n) balanced, O(n) skewed); inorder O(n), Space: O(n)
 */
public class Ques2_ImplementBst {

    static class MyBST {
        private static class Node {
            int val;
            Node left, right;
            Node(int v) { val = v; }
        }

        private Node root;

        public void insert(int x) {
            root = insertInto(root, x);
        }

        private Node insertInto(Node node, int x) {
            if (node == null) return new Node(x);   // fell off the tree -> this is the spot
            if (x < node.val)      node.left  = insertInto(node.left, x);
            else if (x > node.val) node.right = insertInto(node.right, x);
            // x == node.val -> already present, do nothing (no duplicates)
            return node;
        }

        public boolean contains(int x) {
            Node node = root;
            while (node != null) {
                if (x == node.val) return true;
                node = (x < node.val) ? node.left : node.right;
            }
            return false;
        }

        public void delete(int x) {
            root = deleteFrom(root, x);
        }

        private Node deleteFrom(Node node, int x) {
            if (node == null) return null;          // not found
            if (x < node.val) {
                node.left = deleteFrom(node.left, x);
            } else if (x > node.val) {
                node.right = deleteFrom(node.right, x);
            } else {
                // found the node to delete
                if (node.left == null)  return node.right;   // 0 or 1 child (right)
                if (node.right == null) return node.left;    // 1 child (left)
                // two children: copy in inorder successor, then delete it from the right subtree
                Node succ = node.right;
                while (succ.left != null) succ = succ.left;   // min of right subtree
                node.val = succ.val;
                node.right = deleteFrom(node.right, succ.val);
            }
            return node;
        }

        public int[] inorder() {
            List<Integer> out = new ArrayList<>();
            inorder(root, out);
            int[] res = new int[out.size()];
            for (int i = 0; i < res.length; i++) res[i] = out.get(i);
            return res;
        }

        private void inorder(Node node, List<Integer> out) {
            if (node == null) return;
            inorder(node.left, out);
            out.add(node.val);
            inorder(node.right, out);
        }
    }

    static void main() {
        MyBST bst = new MyBST();
        for (int x : new int[]{5, 3, 8, 1, 4}) bst.insert(x);
        System.out.println(Arrays.toString(bst.inorder())); // [1, 3, 4, 5, 8]
        System.out.println(bst.contains(4));                 // true
        System.out.println(bst.contains(9));                 // false
        bst.delete(3);
        System.out.println(Arrays.toString(bst.inorder())); // [1, 4, 5, 8]
    }
}
