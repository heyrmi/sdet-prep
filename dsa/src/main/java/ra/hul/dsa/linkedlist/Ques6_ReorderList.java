package ra.hul.dsa.linkedlist;

/**
 * Reorder List - Reorder L0->L1->...->Ln-1 into L0->Ln-1->L1->Ln-2->... in place.
 * Find the middle, reverse the second half, then weave the two halves.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques6_ReorderList {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static void reorderList(ListNode head) {
        if (head == null || head.next == null) return;

        // 1) find middle: slow ends at the end of the first half
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        // 2) reverse the second half, cutting it off from the first
        ListNode second = slow.next;
        slow.next = null;                 // terminate the first half
        ListNode prev = null;
        while (second != null) {
            ListNode nxt = second.next;
            second.next = prev;
            prev = second;
            second = nxt;
        }
        second = prev;                    // head of reversed second half

        // 3) weave first and second, alternating one node from each
        ListNode first = head;
        while (second != null) {
            ListNode f = first.next, sNext = second.next;
            first.next = second;
            second.next = f;
            first = f;
            second = sNext;
        }
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
        ListNode a = buildList(1, 2, 3, 4);
        reorderList(a);
        System.out.println(toStr(a));   // 1 -> 4 -> 2 -> 3

        ListNode b = buildList(1, 2, 3, 4, 5);
        reorderList(b);
        System.out.println(toStr(b));   // 1 -> 5 -> 2 -> 4 -> 3

        ListNode c = buildList(1, 2);
        reorderList(c);
        System.out.println(toStr(c));   // 1 -> 2
    }
}
