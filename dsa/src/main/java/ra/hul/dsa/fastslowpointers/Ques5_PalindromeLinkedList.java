package ra.hul.dsa.fastslowpointers;

/**
 * Palindrome Linked List - Check if a list reads the same forwards and backwards in O(1) space.
 * Find the middle, reverse the second half, then compare the halves inward.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques5_PalindromeLinkedList {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static boolean isPalindrome(ListNode head) {
        if (head == null || head.next == null) return true;

        // 1) slow ends at the end of the first half
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        // 2) reverse the second half (everything after slow)
        ListNode prev = null, cur = slow.next;
        while (cur != null) {
            ListNode nxt = cur.next; // save before overwriting
            cur.next = prev;
            prev = cur;
            cur = nxt;
        }
        // prev is the head of the reversed second half

        // 3) compare first half (from head) with reversed second half (from prev)
        ListNode first = head, second = prev;
        while (second != null) {
            if (first.val != second.val) return false;
            first = first.next;
            second = second.next;
        }
        return true;
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

    static void main() {
        System.out.println(isPalindrome(buildList(1, 2, 2, 1)));    // true
        System.out.println(isPalindrome(buildList(1, 2, 3, 2, 1))); // true
        System.out.println(isPalindrome(buildList(1, 2)));         // false
        System.out.println(isPalindrome(buildList(1, 2, 1)));      // true
        System.out.println(isPalindrome(buildList(1)));           // true
        System.out.println(isPalindrome(buildList()));            // true
    }
}
