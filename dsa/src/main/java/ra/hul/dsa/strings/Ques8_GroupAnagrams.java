package ra.hul.dsa.strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Group Anagrams - group strings that are anagrams of each other, bucketing by a sorted-character key.
 *
 * Time: O(n * k log k) for n strings of length up to k. Space: O(n * k)
 */
public class Ques8_GroupAnagrams {

    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String word : strs) {
            char[] chars = word.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars); // canonical key shared by all anagrams
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(groups.values());
    }

    static void main() {
        List<List<String>> res = groupAnagrams(new String[]{"eat", "tea", "tan", "ate", "nat", "bat"});
        // Sort within and across groups for a stable, verifiable printout (order is otherwise arbitrary).
        res.forEach(Collections::sort);
        res.sort((x, y) -> x.toString().compareTo(y.toString()));
        System.out.println(res); // [[ate, eat, tea], [bat], [nat, tan]]
    }
}
