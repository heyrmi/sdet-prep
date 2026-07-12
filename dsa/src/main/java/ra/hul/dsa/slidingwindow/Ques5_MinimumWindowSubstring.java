package ra.hul.dsa.slidingwindow;

/**
 * Minimum Window Substring - smallest substring of s containing all characters of t (with multiplicity).
 *
 * Time: O(|s| + |t|), Space: O(alphabet)
 */
public class Ques5_MinimumWindowSubstring {

    public static String minWindow(String s, String t) {
        if (s.length() < t.length() || t.isEmpty()) return "";

        int[] need = new int[128];
        int required = 0;
        for (int i = 0; i < t.length(); i++) {
            if (need[t.charAt(i)]++ == 0) required++; // count distinct required chars
        }

        int left = 0, formed = 0;
        int bestLen = Integer.MAX_VALUE, bestStart = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            need[c]--;                        // one fewer of c still needed by the window
            if (need[c] == 0) formed++;       // this char's requirement is now fully met

            // While the window is valid, shrink it from the left, recording the smallest.
            while (formed == required) {
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1;
                    bestStart = left;
                }
                char lc = s.charAt(left);
                need[lc]++;                   // putting lc back means the window may need it
                if (need[lc] > 0) formed--;   // requirement no longer met -> window invalid
                left++;
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
    }

    static void main() {
        System.out.println(minWindow("ADOBECODEBANC", "ABC")); // BANC
        System.out.println(minWindow("a", "a"));               // a
        System.out.println(minWindow("a", "aa"));              // (empty)
        System.out.println(minWindow("ab", "b"));              // b
    }
}
