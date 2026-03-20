package ra.hul.dsa.hashmap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * First Unique Character in a String - Return the index of first non-repeating character.
 * LeetCode #387 (Easy)
 *
 * Time: O(n), Space: O(1) - at most 26 lowercase letters in map
 */
public class Ques1_FirstUniqueCharacter {

    public static int firstUniqChar(String s) {
        Map<Character, Integer> freq = new LinkedHashMap<>();
        for (char c : s.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        for (int i = 0; i < s.length(); i++) {
            if (freq.get(s.charAt(i)) == 1) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(firstUniqChar("leetcode"));     // 0 ('l')
        System.out.println(firstUniqChar("loveleetcode")); // 2 ('v')
        System.out.println(firstUniqChar("aabb"));         // -1
    }
}
