package ra.hul.dsa.design;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * LFU Cache - Least Frequently Used cache with O(1) get/put.
 * LeetCode #460 (Hard)
 *
 * Strategy: keep two maps.
 *   values    : key   -> value
 *   counts    : key   -> use frequency
 *   freqLists : freq  -> LinkedHashSet of keys at that frequency (insertion order = recency,
 *               so ties within a frequency break by least-recently-used).
 * Track minFreq so eviction is O(1): drop the first key in freqLists[minFreq].
 *
 * Time: O(1) get, O(1) put, Space: O(capacity)
 */
public class Ques2_LFUCache {

    private final int capacity;
    private int minFreq;
    private final Map<Integer, Integer> values = new HashMap<>();
    private final Map<Integer, Integer> counts = new HashMap<>();
    private final Map<Integer, LinkedHashSet<Integer>> freqLists = new HashMap<>();

    public Ques2_LFUCache(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
    }

    private void bump(int key) {
        int f = counts.get(key);
        counts.put(key, f + 1);
        freqLists.get(f).remove(key);
        if (f == minFreq && freqLists.get(f).isEmpty()) {
            minFreq++;
        }
        freqLists.computeIfAbsent(f + 1, k -> new LinkedHashSet<>()).add(key);
    }

    public int get(int key) {
        if (!values.containsKey(key)) return -1;
        bump(key);
        return values.get(key);
    }

    public void put(int key, int value) {
        if (capacity <= 0) return;

        if (values.containsKey(key)) {
            values.put(key, value);
            bump(key);
            return;
        }

        if (values.size() == capacity) {
            LinkedHashSet<Integer> minList = freqLists.get(minFreq);
            int evict = minList.iterator().next(); // least-recently-used among least-frequent
            minList.remove(evict);
            values.remove(evict);
            counts.remove(evict);
        }

        values.put(key, value);
        counts.put(key, 1);
        freqLists.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    static void main() {
        // Classic LeetCode 460 example
        Ques2_LFUCache cache = new Ques2_LFUCache(2);
        StringBuilder actual = new StringBuilder();
        cache.put(1, 1);
        cache.put(2, 2);
        actual.append(cache.get(1)).append(' ');   // 1
        cache.put(3, 3);                            // evicts key 2 (freq 1, LRU)
        actual.append(cache.get(2)).append(' ');   // -1 (not found)
        actual.append(cache.get(3)).append(' ');   // 3
        cache.put(4, 4);                            // evicts key 1 (freq 2 vs 3's freq 2; 1 is LRU)
        actual.append(cache.get(1)).append(' ');   // -1 (not found)
        actual.append(cache.get(3)).append(' ');   // 3
        actual.append(cache.get(4));                // 4

        String expected = "1 -1 3 -1 3 4";
        System.out.println("get results: " + actual);
        System.out.println("expected   : " + expected);
        System.out.println(actual.toString().equals(expected) ? "PASSED" : "FAILED");
    }
}
