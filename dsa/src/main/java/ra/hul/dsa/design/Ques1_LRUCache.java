package ra.hul.dsa.design;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU Cache - Least Recently Used cache with O(1) get/put via HashMap + doubly linked list.
 * LeetCode #146 (Medium)
 *
 * The doubly linked list keeps nodes in recency order: head = most-recent, tail = least-recent.
 * The map gives O(1) lookup from key to its node; on eviction we drop the node before the tail.
 *
 * Time: O(1) get, O(1) put, Space: O(capacity)
 */
public class Ques1_LRUCache {

    static class Node {
        int key, val;
        Node prev, next;
        Node(int key, int val) { this.key = key; this.val = val; }
    }

    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head; // sentinel: head.next is most-recently used
    private final Node tail; // sentinel: tail.prev is least-recently used

    public Ques1_LRUCache(int capacity) {
        this.capacity = capacity;
        head = new Node(0, 0);
        tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }

    // unlink a node from the list
    private void remove(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // insert node right after head (mark as most-recently used)
    private void addFront(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        remove(node);
        addFront(node);
        return node.val;
    }

    public void put(int key, int value) {
        Node existing = map.get(key);
        if (existing != null) {
            existing.val = value;
            remove(existing);
            addFront(existing);
            return;
        }
        if (map.size() == capacity) {
            Node lru = tail.prev; // least-recently used
            remove(lru);
            map.remove(lru.key);
        }
        Node node = new Node(key, value);
        map.put(key, node);
        addFront(node);
    }

    static void main() {
        // Classic LeetCode 146 example
        Ques1_LRUCache cache = new Ques1_LRUCache(2);
        StringBuilder actual = new StringBuilder();
        cache.put(1, 1);
        cache.put(2, 2);
        actual.append(cache.get(1)).append(' ');   // 1
        cache.put(3, 3);                            // evicts key 2
        actual.append(cache.get(2)).append(' ');   // -1 (not found)
        cache.put(4, 4);                            // evicts key 1
        actual.append(cache.get(1)).append(' ');   // -1 (not found)
        actual.append(cache.get(3)).append(' ');   // 3
        actual.append(cache.get(4));                // 4

        String expected = "1 -1 -1 3 4";
        System.out.println("get results: " + actual);
        System.out.println("expected   : " + expected);
        System.out.println(actual.toString().equals(expected) ? "PASSED" : "FAILED");
    }
}
