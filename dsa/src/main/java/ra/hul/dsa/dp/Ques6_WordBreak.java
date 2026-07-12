package ra.hul.dsa.dp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Word Break - Can s be segmented into a sequence of dictionary words (reusable)?
 *
 * Time: O(n^2 * L), Space: O(n) - dp[i] = OR over j of (dp[j] && s[j..i) in dict).
 */
public class Ques6_WordBreak {

    public static boolean wordBreak(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);   // O(1) membership
        int n = s.length();
        boolean[] dp = new boolean[n + 1];            // 1-indexed; defaults to false
        dp[0] = true;                                 // empty prefix is segmentable
        for (int i = 1; i <= n; i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j] && dict.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break;                            // one valid split is enough
                }
            }
        }
        return dp[n];
    }

    static void main() {
        System.out.println(wordBreak("leetcode", Arrays.asList("leet", "code")));                      // true
        System.out.println(wordBreak("applepenapple", Arrays.asList("apple", "pen")));                 // true
        System.out.println(wordBreak("catsandog", Arrays.asList("cats", "dog", "sand", "and", "cat"))); // false
    }
}
