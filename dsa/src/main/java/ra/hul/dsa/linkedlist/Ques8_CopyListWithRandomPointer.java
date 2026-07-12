package ra.hul.dsa.linkedlist;

/**
 * Copy List with Random Pointer - Deep-copy a list where each node also has a random pointer.
 * LeetCode #138 (Medium)
 *
 * Interleave approach, O(1) extra space: (1) insert each copy right after its original, (2) wire the
 * copies' random pointers via orig.next (the copy) from orig.random.next, (3) unweave the two lists.
 *
 * Time: O(n), Space: O(1) extra
 */
public class Ques8_CopyListWithRandomPointer {

    static class Node {
        int val;
        Node next;
        Node random;
        Node(int val) { this.val = val; }
    }

    public static Node copyRandomList(Node head) {
        if (head == null) return null;

        // 1) interleave copies: A -> A' -> B -> B' -> ...
        for (Node cur = head; cur != null; cur = cur.next.next) {
            Node copy = new Node(cur.val);
            copy.next = cur.next;
            cur.next = copy;
        }

        // 2) assign randoms on the copies
        for (Node cur = head; cur != null; cur = cur.next.next) {
            if (cur.random != null) {
                cur.next.random = cur.random.next;
            }
        }

        // 3) unweave into original and copied lists
        Node dummy = new Node(0);
        Node copyTail = dummy;
        for (Node cur = head; cur != null; cur = cur.next) {
            Node copy = cur.next;
            cur.next = copy.next;      // restore original list
            copyTail.next = copy;      // build copied list
            copyTail = copy;
        }
        return dummy.next;
    }

    static void main() {
        // Build: 1 -> 2 -> 3, with randoms: 1.random=3, 2.random=1, 3.random=null
        Node n1 = new Node(1), n2 = new Node(2), n3 = new Node(3);
        n1.next = n2; n2.next = n3;
        n1.random = n3;
        n2.random = n1;
        n3.random = null;

        Node copy = copyRandomList(n1);

        boolean ok = true;
        // structure check
        ok &= copy != n1 && copy.val == 1 && copy.next.val == 2 && copy.next.next.val == 3;
        // randoms are deep copies (different objects) pointing at the right values
        ok &= copy.random != null && copy.random != n3 && copy.random.val == 3;
        ok &= copy.next.random != null && copy.next.random != n1 && copy.next.random.val == 1;
        ok &= copy.next.next.random == null;
        // original list must be intact
        ok &= n1.next == n2 && n2.next == n3 && n1.random == n3 && n2.random == n1;

        System.out.println("copy: " + copy.val + " -> " + copy.next.val + " -> " + copy.next.next.val);
        System.out.println("copy.random.val=" + copy.random.val + " (expected 3, distinct object: "
                + (copy.random != n3) + ")");
        System.out.println("copy.next.random.val=" + copy.next.random.val + " (expected 1)");
        System.out.println("original intact: " + (n1.next == n2 && n2.next == n3));

        // null list
        ok &= copyRandomList(null) == null;

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
