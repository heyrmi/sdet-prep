package ra.hul.dsa.slidingwindow;

import java.util.Arrays;

/**
 * Permutation in String - does s2 contain a permutation of s1 as a substring?
 *
 * Time: O(|s1| + |s2|), Space: O(1) - two 26-arrays
 */
public class Ques4_PermutationInString {

    public static boolean checkInclusion(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        if (m > n) return false;

        int[] need = new int[26];
        int[] window = new int[26];
        for (int i = 0; i < m; i++) {
            need[s1.charAt(i) - 'a']++;
            window[s2.charAt(i) - 'a']++; // seed the first window of width m
        }
        if (Arrays.equals(need, window)) return true;

        for (int right = m; right < n; right++) {
            window[s2.charAt(right) - 'a']++;          // element enters on the right
            window[s2.charAt(right - m) - 'a']--;      // element leaves on the left
            if (Arrays.equals(need, window)) return true;
        }
        return false;
    }

    static void main() {
        System.out.println(checkInclusion("ab", "eidbaooo")); // true
        System.out.println(checkInclusion("ab", "eidboaoo")); // false
        System.out.println(checkInclusion("adc", "dcda"));    // true
    }
}
