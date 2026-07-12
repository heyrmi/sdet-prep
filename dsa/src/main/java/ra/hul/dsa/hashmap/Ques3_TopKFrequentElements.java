package ra.hul.dsa.hashmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top K Frequent Elements - return the k most frequent elements (any order).
 *
 * Time: O(n), Space: O(n) - frequency map + bucket sort
 */
public class Ques3_TopKFrequentElements {

    public static int[] topKFrequent(int[] nums, int k) {
        // Step 1: frequency map value -> count.
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : nums) counts.merge(v, 1, Integer::sum);

        // Step 2: bucket sort by frequency. A value can appear at most nums.length times.
        @SuppressWarnings("unchecked")
        List<Integer>[] buckets = new List[nums.length + 1];
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            int freq = e.getValue();
            if (buckets[freq] == null) buckets[freq] = new ArrayList<>();
            buckets[freq].add(e.getKey());
        }

        // Sweep from highest frequency down, collecting until we have k values.
        int[] result = new int[k];
        int idx = 0;
        for (int freq = buckets.length - 1; freq >= 1 && idx < k; freq--) {
            if (buckets[freq] == null) continue;
            for (int value : buckets[freq]) {
                result[idx++] = value;
                if (idx == k) break;
            }
        }
        return result;
    }

    static void main() {
        System.out.println(Arrays.toString(topKFrequent(new int[]{1, 1, 1, 2, 2, 3}, 2)));   // [1, 2]
        System.out.println(Arrays.toString(topKFrequent(new int[]{1}, 1)));                   // [1]
        System.out.println(Arrays.toString(topKFrequent(new int[]{4, 4, 4, 5, 5, 6}, 2)));    // [4, 5]
    }
}
