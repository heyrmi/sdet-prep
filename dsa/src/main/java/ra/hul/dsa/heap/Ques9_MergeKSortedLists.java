package ra.hul.dsa.heap;

import java.util.PriorityQueue;

/**
 * Merge K Sorted Lists - K-way merge with a min-heap of the current list heads.
 * LeetCode #23 (Hard)
 *
 * Time: O(N log k), Space: O(k)
 */
public class Ques9_MergeKSortedLists {

    static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    public static ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> heap =
                new PriorityQueue<>((a, b) -> Integer.compare(a.val, b.val));

        if (lists != null) {
            for (ListNode head : lists) {
                if (head != null) heap.offer(head); // never offer null
            }
        }

        ListNode dummy = new ListNode();
        ListNode tail = dummy;
        while (!heap.isEmpty()) {
            ListNode node = heap.poll();      // smallest current head
            tail.next = node;
            tail = node;
            if (node.next != null) heap.offer(node.next);
        }
        tail.next = null;                     // terminate cleanly
        return dummy.next;
    }

    // Build a linked list from the given values.
    private static ListNode build(int... vals) {
        ListNode dummy = new ListNode();
        ListNode tail = dummy;
        for (int v : vals) { tail.next = new ListNode(v); tail = tail.next; }
        return dummy.next;
    }

    // Render a linked list as "1->1->2->..." for a readable printout.
    private static String toString(ListNode head) {
        StringBuilder sb = new StringBuilder();
        while (head != null) {
            sb.append(head.val);
            if (head.next != null) sb.append("->");
            head = head.next;
        }
        return sb.toString();
    }

    static void main() {
        ListNode[] lists = {build(1, 4, 5), build(1, 3, 4), build(2, 6)};
        System.out.println(toString(mergeKLists(lists))); // 1->1->2->3->4->4->5->6
    }
}
