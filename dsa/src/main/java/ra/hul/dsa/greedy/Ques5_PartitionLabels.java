package ra.hul.dsa.greedy;

import java.util.ArrayList;
import java.util.List;

/**
 * Partition Labels - split s into as many parts as possible so each letter appears in one part.
 *
 * Time: O(n), Space: O(1) - last-occurrence table over 26 letters
 */
public class Ques5_PartitionLabels {

    public static List<Integer> partitionLabels(String s) {
        int[] last = new int[26];
        for (int i = 0; i < s.length(); i++) {
            last[s.charAt(i) - 'a'] = i;   // overwrite => ends on the final occurrence
        }
        List<Integer> sizes = new ArrayList<>();
        int start = 0, end = 0;
        for (int i = 0; i < s.length(); i++) {
            end = Math.max(end, last[s.charAt(i) - 'a']); // stretch the window
            if (i == end) {                 // window is self-contained -> cut
                sizes.add(i - start + 1);
                start = i + 1;
            }
        }
        return sizes;
    }

    static void main() {
        System.out.println(partitionLabels("ababcbacadefegdehijhklij")); // [9, 7, 8]
        System.out.println(partitionLabels("eccbbbbdec"));               // [10]
        System.out.println(partitionLabels("a"));                        // [1]
    }
}
