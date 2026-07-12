package ra.hul.dsa.slidingwindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Find All Anagrams in a String - start indices of every substring of s that is an anagram of p.
 *
 * Time: O(|s| + |p|), Space: O(1) - two 26-arrays
 */
public class Ques6_FindAllAnagramsInAString {

    public static List<Integer> findAnagrams(String s, String p) {
        List<Integer> result = new ArrayList<>();
        int m = p.length(), n = s.length();
        if (m > n) return result;

        int[] need = new int[26];
        int[] window = new int[26];
        for (int i = 0; i < m; i++) {
            need[p.charAt(i) - 'a']++;
            window[s.charAt(i) - 'a']++; // seed the first window of width m
        }
        if (Arrays.equals(need, window)) result.add(0);

        for (int right = m; right < n; right++) {
            window[s.charAt(right) - 'a']++;       // element enters on the right
            window[s.charAt(right - m) - 'a']--;   // element leaves on the left
            if (Arrays.equals(need, window)) result.add(right - m + 1);
        }
        return result;
    }

    static void main() {
        System.out.println(findAnagrams("cbaebabacd", "abc")); // [0, 6]
        System.out.println(findAnagrams("abab", "ab"));        // [0, 1, 2]
        System.out.println(findAnagrams("aa", "bb"));          // []
    }
}
