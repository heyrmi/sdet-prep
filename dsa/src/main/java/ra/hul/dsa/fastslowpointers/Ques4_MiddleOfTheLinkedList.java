package ra.hul.dsa.fastslowpointers;

/**
 * Middle of the Linked List - Return the middle node (the second of two middles on even length) in one pass.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques4_MiddleOfTheLinkedList {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static ListNode middleNode(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) { // fast != null FIRST (short-circuit)
            slow = slow.next;        // +1
            fast = fast.next.next;   // +2
        }
        return slow; // middle; on even length, the second of the two middles
    }

    static ListNode buildList(int... vals) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        for (int v : vals) {
            curr.next = new ListNode(v);
            curr = curr.next;
        }
        return dummy.next;
    }

    static String show(ListNode n) {
        return n == null ? "null" : String.valueOf(n.val);
    }

    static void main() {
        System.out.println(show(middleNode(buildList(1, 2, 3, 4, 5))));    // 3
        System.out.println(show(middleNode(buildList(1, 2, 3, 4, 5, 6)))); // 4
        System.out.println(show(middleNode(buildList(1))));               // 1
        System.out.println(show(middleNode(buildList(1, 2))));            // 2
        System.out.println(show(middleNode(buildList())));               // null
    }
}
