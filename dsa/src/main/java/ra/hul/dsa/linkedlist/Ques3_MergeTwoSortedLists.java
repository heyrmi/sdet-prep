package ra.hul.dsa.linkedlist;

/**
 * Merge Two Sorted Lists - Splice two sorted lists into one sorted list using a dummy head.
 *
 * Time: O(n + m), Space: O(1)
 */
public class Ques3_MergeTwoSortedLists {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0), tail = dummy;
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) { tail.next = l1; l1 = l1.next; }
            else                  { tail.next = l2; l2 = l2.next; }
            tail = tail.next;
        }
        tail.next = (l1 != null) ? l1 : l2; // one of them is null; attach the rest whole
        return dummy.next;
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
        System.out.println(toStr(mergeTwoLists(buildList(1, 2, 4), buildList(1, 3, 4)))); // 1 -> 1 -> 2 -> 3 -> 4 -> 4
        System.out.println(toStr(mergeTwoLists(buildList(), buildList(0))));              // 0
        System.out.println(toStr(mergeTwoLists(buildList(), buildList())));               // (empty)
    }
}
