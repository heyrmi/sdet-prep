package ra.hul.dsa.hashmap;

/**
 * Implement a HashMap (Separate Chaining) - build an int->int map from scratch using buckets + chains.
 *
 * Time: O(1) average per operation, O(n) worst case; Space: O(n)
 */
public class Ques2_ImplementHashMap {

    /** A hash map with separate chaining. int keys, int values. */
    static class MyHashMap {

        private static final int NUM_BUCKETS = 769; // prime for an even spread

        private final Node[] buckets;

        MyHashMap() {
            buckets = new Node[NUM_BUCKETS];
        }

        private int indexFor(int key) {
            // floorMod gives a non-negative result even for negative keys.
            return Math.floorMod(key, NUM_BUCKETS);
        }

        void put(int key, int value) {
            int i = indexFor(key);
            for (Node n = buckets[i]; n != null; n = n.next) {
                if (n.key == key) {       // existing key -> update in place
                    n.value = value;
                    return;
                }
            }
            // not found -> prepend a new node (O(1))
            Node node = new Node(key, value);
            node.next = buckets[i];
            buckets[i] = node;
        }

        int get(int key) {
            int i = indexFor(key);
            for (Node n = buckets[i]; n != null; n = n.next) {
                if (n.key == key) return n.value;
            }
            return -1; // sentinel for "absent"
        }

        void remove(int key) {
            int i = indexFor(key);
            Node prev = null;
            for (Node n = buckets[i]; n != null; prev = n, n = n.next) {
                if (n.key == key) {
                    if (prev == null) buckets[i] = n.next; // removing the head
                    else prev.next = n.next;
                    return;
                }
            }
        }

        boolean containsKey(int key) {
            int i = indexFor(key);
            for (Node n = buckets[i]; n != null; n = n.next) {
                if (n.key == key) return true;
            }
            return false;
        }
    }

    /** A node in a bucket's chain. */
    static class Node {
        int key;
        int value;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    static void main() {
        MyHashMap map = new MyHashMap();
        map.put(1, 10);
        map.put(2, 20);
        System.out.println(map.get(1));         // 10
        System.out.println(map.get(3));         // -1
        map.put(2, 99);                         // update, not duplicate
        System.out.println(map.get(2));         // 99
        map.remove(2);
        System.out.println(map.containsKey(2)); // false
    }
}
