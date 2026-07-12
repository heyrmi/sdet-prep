package ra.hul.dsa.linkedlist;

/**
 * Remove Nth Node From End of List - One-pass removal with a gap-of-n two pointers and a dummy head.
 *
 * Time: O(L), Space: O(1)
 */
public class Ques5_RemoveNthFromEnd {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode fast = dummy, slow = dummy;
        for (int i = 0; i < n; i++) fast = fast.next;   // open a gap of n
        while (fast.next != null) {                     // until fast is the last node
            fast = fast.next;
            slow = slow.next;
        }
        slow.next = slow.next.next;                     // unlink the target
        return dummy.next;                              // handles head removal too
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

    static String toStr(ListNode head) {
        StringBuilder sb = new StringBuilder();
        while (head != null) {
            sb.append(head.val);
            if (head.next != null) sb.append(" -> ");
            head = head.next;
        }
        return sb.length() == 0 ? "(empty)" : sb.toString();
    }

    static void main() {
        System.out.println(toStr(removeNthFromEnd(buildList(1, 2, 3, 4, 5), 2))); // 1 -> 2 -> 3 -> 5
        System.out.println(toStr(removeNthFromEnd(buildList(1), 1)));             // (empty)
        System.out.println(toStr(removeNthFromEnd(buildList(1, 2), 1)));          // 1
        System.out.println(toStr(removeNthFromEnd(buildList(1, 2), 2)));          // 2
    }
}
