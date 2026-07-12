package ra.hul.dsa.linkedlist;

/**
 * Add Two Numbers - Add two numbers stored as reversed-digit linked lists.
 * LeetCode #2 (Medium)
 *
 * Walk both lists simultaneously adding digit-by-digit with a carry, exactly like grade-school
 * addition; the reversed storage means we process least-significant digits first.
 *
 * Time: O(max(m, n)), Space: O(max(m, n))
 */
public class Ques7_AddTwoNumbers {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        int carry = 0;
        while (l1 != null || l2 != null || carry != 0) {
            int sum = carry;
            if (l1 != null) { sum += l1.val; l1 = l1.next; }
            if (l2 != null) { sum += l2.val; l2 = l2.next; }
            carry = sum / 10;
            curr.next = new ListNode(sum % 10);
            curr = curr.next;
        }
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
        boolean ok = true;

        // 342 + 465 = 807  -> [2,4,3] + [5,6,4] = [7,0,8]
        ListNode r1 = addTwoNumbers(buildList(2, 4, 3), buildList(5, 6, 4));
        System.out.println(toStr(r1) + " expected 7 -> 0 -> 8");
        ok &= toStr(r1).equals("7 -> 0 -> 8");

        // 0 + 0 = 0
        ListNode r2 = addTwoNumbers(buildList(0), buildList(0));
        System.out.println(toStr(r2) + " expected 0");
        ok &= toStr(r2).equals("0");

        // 9999999 + 9999 = 10009998
        ListNode r3 = addTwoNumbers(buildList(9, 9, 9, 9, 9, 9, 9), buildList(9, 9, 9, 9));
        System.out.println(toStr(r3) + " expected 8 -> 9 -> 9 -> 9 -> 0 -> 0 -> 0 -> 1");
        ok &= toStr(r3).equals("8 -> 9 -> 9 -> 9 -> 0 -> 0 -> 0 -> 1");

        System.out.println(ok ? "PASSED" : "FAILED");
    }
}
