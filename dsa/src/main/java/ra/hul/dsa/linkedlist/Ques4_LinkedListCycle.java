package ra.hul.dsa.linkedlist;

/**
 * Linked List Cycle - Detect whether a linked list contains a cycle using Floyd's fast/slow pointers.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques4_LinkedListCycle {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) { // fast != null FIRST (short-circuit)
            slow = slow.next;        // +1
            fast = fast.next.next;   // +2
            if (slow == fast) return true; // same node object -> cycle
        }
        return false; // fast reached the end -> no cycle
    }

    /** Build a list; if pos >= 0 the tail's next links back to the node at index pos. */
    static ListNode buildWithCycle(int[] vals, int pos) {
        if (vals.length == 0) return null;
        ListNode[] nodes = new ListNode[vals.length];
        for (int i = 0; i < vals.length; i++) nodes[i] = new ListNode(vals[i]);
        for (int i = 0; i < vals.length - 1; i++) nodes[i].next = nodes[i + 1];
        if (pos >= 0) nodes[vals.length - 1].next = nodes[pos];
        return nodes[0];
    }

    static void main() {
        System.out.println(hasCycle(buildWithCycle(new int[]{3, 2, 0, -4}, 1)));  // true
        System.out.println(hasCycle(buildWithCycle(new int[]{1, 2}, 0)));         // true
        System.out.println(hasCycle(buildWithCycle(new int[]{1}, -1)));           // false
        System.out.println(hasCycle(buildWithCycle(new int[]{}, -1)));            // false
    }
}
