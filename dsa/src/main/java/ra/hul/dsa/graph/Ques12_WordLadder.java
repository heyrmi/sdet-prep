package ra.hul.dsa.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Word Ladder - shortest transformation length changing one letter at a time (BFS on implicit graph).
 * LeetCode #127 (Hard)
 *
 * Time: O(N * L * 26), Space: O(N * L)
 */
public class Ques12_WordLadder {

    public static int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> words = new HashSet<>(wordList);
        if (!words.contains(endWord)) return 0;

        Deque<String> queue = new ArrayDeque<>();
        queue.add(beginWord);
        words.remove(beginWord);            // don't revisit the start
        int level = 1;                      // sequence length includes beginWord

        while (!queue.isEmpty()) {
            int size = queue.size();        // process exactly one BFS level
            for (int i = 0; i < size; i++) {
                String word = queue.poll();
                if (word.equals(endWord)) return level;

                char[] arr = word.toCharArray();
                for (int pos = 0; pos < arr.length; pos++) {
                    char original = arr[pos];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == original) continue;
                        arr[pos] = c;
                        String next = new String(arr);
                        if (words.contains(next)) {
                            words.remove(next);   // mark visited on enqueue
                            queue.add(next);
                        }
                    }
                    arr[pos] = original;          // restore before next position
                }
            }
            level++;
        }
        return 0;                           // endWord unreachable
    }

    static void main() {
        System.out.println(ladderLength("hit", "cog",
            Arrays.asList("hot", "dot", "dog", "lot", "log", "cog")));   // 5
        System.out.println(ladderLength("hit", "cog",
            Arrays.asList("hot", "dot", "dog", "lot", "log")));          // 0
    }
}
