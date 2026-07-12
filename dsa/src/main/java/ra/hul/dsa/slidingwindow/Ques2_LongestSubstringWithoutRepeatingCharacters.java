package ra.hul.dsa.slidingwindow;

import java.util.HashMap;
import java.util.Map;

/**
 * Longest Substring Without Repeating Characters - length of the longest substring with all distinct chars.
 *
 * Time: O(n), Space: O(min(n, charset))
 */
public class Ques2_LongestSubstringWithoutRepeatingCharacters {

    public static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>(); // char -> last index seen
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen.containsKey(c)) {
                // Only jump forward; never move left backward past the current window.
                left = Math.max(left, lastSeen.get(c) + 1);
            }
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static void main() {
        System.out.println(lengthOfLongestSubstring("abcabcbb")); // 3
        System.out.println(lengthOfLongestSubstring("bbbbb"));    // 1
        System.out.println(lengthOfLongestSubstring("pwwkew"));   // 3
        System.out.println(lengthOfLongestSubstring(""));         // 0
    }
}
