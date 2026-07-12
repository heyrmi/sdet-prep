package ra.hul.dsa.fastslowpointers;

/**
 * Linked List Cycle II - Return the node where the cycle begins, or null if there is no cycle (Floyd's).
 *
 * Time: O(n), Space: O(1)
 */
public class Ques1_LinkedListCycleII {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) { // fast != null FIRST (short-circuit)
            slow = slow.next;          // +1
            fast = fast.next.next;     // +2
            if (slow == fast) {        // cycle detected
                ListNode p = head;
                while (p != slow) {    // both move +1 until they meet at the start
                    p = p.next;
                    slow = slow.next;
                }
                return p;              // cycle start
            }
        }
        return null;                   // fast reached the end -> no cycle
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

    static String show(ListNode n) {
        return n == null ? "null" : String.valueOf(n.val);
    }

    static void main() {
        System.out.println(show(detectCycle(buildWithCycle(new int[]{3, 2, 0, -4}, 1)))); // 2
        System.out.println(show(detectCycle(buildWithCycle(new int[]{1, 2}, 0))));        // 1
        System.out.println(show(detectCycle(buildWithCycle(new int[]{1, 2}, -1))));       // null
        System.out.println(show(detectCycle(buildWithCycle(new int[]{1}, 0))));           // 1
        System.out.println(show(detectCycle(buildWithCycle(new int[]{}, -1))));           // null
    }
}
