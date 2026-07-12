package ra.hul.dsa.trie;

import java.util.List;

/**
 * Replace Words - build a trie of roots, replace each word with the shortest root that prefixes it.
 * LeetCode #648 (Medium)
 *
 * Time: O(R + S) (R, S = total chars in roots / sentence), Space: O(R)
 */
public class Ques3_ReplaceWords {

    static class RootNode {
        RootNode[] children = new RootNode[26];
        boolean isEnd = false;
    }

    static String replaceWords(List<String> dictionary, String sentence) {
        RootNode root = new RootNode();
        for (String r : dictionary) {
            insert(root, r);
        }
        String[] words = sentence.split(" ");
        for (int i = 0; i < words.length; i++) {
            words[i] = shortestRoot(root, words[i]);
        }
        return String.join(" ", words);
    }

    private static void insert(RootNode root, String word) {
        RootNode node = root;
        for (int i = 0; i < word.length(); i++) {
            int idx = word.charAt(i) - 'a';
            if (node.children[idx] == null) {
                node.children[idx] = new RootNode();
            }
            node = node.children[idx];
        }
        node.isEnd = true;
    }

    /** Return the shortest root that prefixes word, or word itself if none. */
    private static String shortestRoot(RootNode root, String word) {
        RootNode node = root;
        for (int i = 0; i < word.length(); i++) {
            int idx = word.charAt(i) - 'a';
            if (node.children[idx] == null) {
                return word; // fell off the trie before any root ended
            }
            node = node.children[idx];
            if (node.isEnd) {
                return word.substring(0, i + 1); // shortest root: stop at the first isEnd
            }
        }
        return word; // the word itself is shorter than any stored root, or no root
    }

    static void main() {
        System.out.println(replaceWords(
                List.of("cat", "bat", "rat"),
                "the cattle was rattled by the battery")); // the cat was rat by the bat
        System.out.println(replaceWords(
                List.of("a", "b", "c"),
                "aadsfasf absbs bbab cadsfafs"));          // a a b c
    }
}
