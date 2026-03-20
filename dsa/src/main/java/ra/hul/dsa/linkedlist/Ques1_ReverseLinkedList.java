package ra.hul.dsa.linkedlist;

/**
 * Reverse Linked List - Reverse a singly linked list iteratively.
 * LeetCode #206 (Easy)
 *
 * Time: O(n), Space: O(1)
 */
public class Ques1_ReverseLinkedList {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    public static ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;
        while (curr != null) {
            ListNode next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
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

    static void printList(ListNode head) {
        while (head != null) {
            System.out.print(head.val + (head.next != null ? " -> " : ""));
            head = head.next;
        }
        System.out.println();
    }

    static void main() {
        ListNode list = buildList(1, 2, 3, 4, 5);
        printList(list);               // 1 -> 2 -> 3 -> 4 -> 5
        printList(reverseList(list));   // 5 -> 4 -> 3 -> 2 -> 1
    }
}
