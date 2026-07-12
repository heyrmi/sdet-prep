package ra.hul.dsa.linkedlist;

/**
 * Implement a Singly Linked List - MyLinkedList backed by raw nodes with a dummy head, cached size and tail.
 *
 * Time: addFirst/addLast/size O(1), get/removeAt O(n); Space: O(n)
 */
public class Ques2_ImplementLinkedList {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    /** Singly linked list with a sentinel head plus cached size and tail. */
    static class MyLinkedList {
        private final ListNode dummy = new ListNode(0); // sentinel before the real head
        private ListNode tail = dummy;                  // last node (== dummy when empty)
        private int size = 0;

        /** Insert v as the new head. O(1). */
        public void addFirst(int v) {
            ListNode node = new ListNode(v);
            node.next = dummy.next;
            dummy.next = node;
            if (tail == dummy) tail = node; // list was empty: new node is also the tail
            size++;
        }

        /** Append v at the tail. O(1) with the cached tail. */
        public void addLast(int v) {
            ListNode node = new ListNode(v);
            tail.next = node;
            tail = node;
            size++;
        }

        /** Value at 0-based index, or -1 if out of bounds. O(n). */
        public int get(int index) {
            if (index < 0 || index >= size) return -1;
            ListNode cur = dummy.next;          // the real head
            for (int i = 0; i < index; i++) cur = cur.next;
            return cur.val;
        }

        /** Remove the node at index. No-op if out of bounds. O(n). */
        public void removeAt(int index) {
            if (index < 0 || index >= size) return;
            ListNode prev = dummy;              // node before index (dummy handles index 0)
            for (int i = 0; i < index; i++) prev = prev.next;
            ListNode removed = prev.next;
            prev.next = removed.next;
            if (removed == tail) tail = prev;   // removed the last node: fix the tail
            size--;
        }

        /** Number of elements. O(1). */
        public int size() {
            return size;
        }
    }

    static void main() {
        MyLinkedList list = new MyLinkedList();
        list.addLast(1);
        list.addLast(3);
        list.addFirst(0);              // list = 0 -> 1 -> 3
        System.out.println(list.size());   // 3
        System.out.println(list.get(0));   // 0
        System.out.println(list.get(2));   // 3
        System.out.println(list.get(9));   // -1
        list.removeAt(1);              // list = 0 -> 3
        System.out.println(list.get(1));   // 3
        System.out.println(list.size());   // 2
    }
}
