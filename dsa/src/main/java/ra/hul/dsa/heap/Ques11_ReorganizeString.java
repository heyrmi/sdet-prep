package ra.hul.dsa.heap;

import java.util.PriorityQueue;

/**
 * Reorganize String - rearrange so no two adjacent chars match, using a max-heap by remaining count.
 * LeetCode #767 (Medium)
 *
 * Time: O(n log 26) ~ O(n), Space: O(1)
 */
public class Ques11_ReorganizeString {

    public static String reorganizeString(String s) {
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) count[s.charAt(i) - 'a']++;

        // Max-heap of char codes ordered by remaining count, descending.
        PriorityQueue<Integer> heap = new PriorityQueue<>((a, b) -> Integer.compare(count[b], count[a]));
        for (int c = 0; c < 26; c++) {
            if (count[c] > 0) heap.offer(c);
        }

        StringBuilder sb = new StringBuilder();
        int prev = -1; // the char placed last (cooling down), or -1 if none
        while (!heap.isEmpty()) {
            int cur = heap.poll();          // most frequent available
            sb.append((char) ('a' + cur));
            count[cur]--;

            if (prev != -1 && count[prev] > 0) heap.offer(prev); // prev is free to use again
            prev = (count[cur] > 0) ? cur : -1;                  // hold cur back for one round
        }

        return sb.length() == s.length() ? sb.toString() : "";
    }

    static void main() {
        System.out.println(reorganizeString("aab"));    // aba
        System.out.println(reorganizeString("aaab"));   // (empty string)
        System.out.println(reorganizeString("aaabbc")); // abacba (any valid rearrangement)
    }
}
