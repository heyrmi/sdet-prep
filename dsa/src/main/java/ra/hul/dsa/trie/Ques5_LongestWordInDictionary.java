package ra.hul.dsa.trie;

/**
 * Longest Word in Dictionary - longest word buildable one letter at a time where every prefix is a word.
 * LeetCode #720 (Medium)
 *
 * Time: O(total chars), Space: O(total chars)
 */
public class Ques5_LongestWordInDictionary {

    static class LWNode {
        LWNode[] children = new LWNode[26];
        boolean isEnd = false;
    }

    private static String best = "";

    static String longestWord(String[] words) {
        best = "";
        LWNode root = new LWNode();
        for (String w : words) {
            insert(root, w);
        }
        // The root represents "" (not a word); descend into its word-children.
        dfs(root, new StringBuilder());
        return best;
    }

    private static void insert(LWNode root, String word) {
        LWNode node = root;
        for (int i = 0; i < word.length(); i++) {
            int idx = word.charAt(i) - 'a';
            if (node.children[idx] == null) {
                node.children[idx] = new LWNode();
            }
            node = node.children[idx];
        }
        node.isEnd = true;
    }

    private static void dfs(LWNode node, StringBuilder path) {
        // Iterate a..z so equal-length ties keep the lexicographically smallest (found first).
        for (int i = 0; i < 26; i++) {
            LWNode child = node.children[i];
            if (child == null || !child.isEnd) {
                continue; // can only build through words: every prefix must be a word
            }
            path.append((char) ('a' + i));
            if (path.length() > best.length()) { // strictly longer; a..z order handles ties
                best = path.toString();
            }
            dfs(child, path);
            path.deleteCharAt(path.length() - 1); // backtrack
        }
    }

    static void main() {
        System.out.println(longestWord(
                new String[]{"w", "wo", "wor", "worl", "world"})); // world
        System.out.println(longestWord(
                new String[]{"a", "banana", "app", "appl", "ap", "apply", "apple"})); // apple
    }
}
