package ra.hul.dsa.heap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Top K Frequent Words - k most frequent words, ties broken alphabetically, via a size-k min-heap.
 * LeetCode #692 (Medium)
 *
 * Time: O(D log k) where D = distinct words, Space: O(D)
 */
public class Ques8_TopKFrequentWords {

    public static List<String> topKFrequent(String[] words, int k) {
        Map<String, Integer> count = new HashMap<>();
        for (String w : words) count.merge(w, 1, Integer::sum);

        // Root = worst-of-the-k: smaller count first; for ties, the larger word first (evict it).
        PriorityQueue<String> heap = new PriorityQueue<>((a, b) ->
                count.get(a).equals(count.get(b))
                        ? b.compareTo(a)                 // equal count: larger word is "worse"
                        : count.get(a) - count.get(b));  // counts are small positives -> safe

        for (String w : count.keySet()) {
            heap.offer(w);
            if (heap.size() > k) heap.poll(); // drop the worst-of-the-(k+1)
        }

        // Heap pops worst->best (low count / large word first); reverse for best->worst.
        LinkedList<String> result = new LinkedList<>();
        while (!heap.isEmpty()) result.addFirst(heap.poll());
        return result;
    }

    static void main() {
        System.out.println(topKFrequent(
                new String[]{"i", "love", "leetcode", "i", "love", "coding"}, 2)); // [i, love]
        System.out.println(topKFrequent(
                new String[]{"the", "day", "is", "sunny", "the", "the", "the", "sunny", "is", "is"}, 4)); // [the, is, sunny, day]
    }
}
