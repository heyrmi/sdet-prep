package ra.hul.dsa.slidingwindow;

/**
 * Longest Repeating Character Replacement - longest single-letter substring after replacing at most k chars.
 *
 * Time: O(n), Space: O(1) - 26 counters
 */
public class Ques3_LongestRepeatingCharacterReplacement {

    public static int characterReplacement(String s, int k) {
        int[] count = new int[26]; // frequency of each letter in the window
        int left = 0, maxFreq = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            int idx = s.charAt(right) - 'A';
            count[idx]++;
            maxFreq = Math.max(maxFreq, count[idx]);
            // If we'd need more than k replacements, slide the left edge forward by one.
            if ((right - left + 1) - maxFreq > k) {
                count[s.charAt(left) - 'A']--;
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static void main() {
        System.out.println(characterReplacement("ABAB", 2));    // 4
        System.out.println(characterReplacement("AABABBA", 1)); // 4
        System.out.println(characterReplacement("AAAA", 0));    // 4
    }
}
