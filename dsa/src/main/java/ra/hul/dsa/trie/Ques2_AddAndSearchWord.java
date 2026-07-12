package ra.hul.dsa.trie;

/**
 * Add and Search Word - a trie where '.' in a query matches any single letter (branching DFS).
 * LeetCode #211 (Medium)
 *
 * Time: addWord O(L), search O(26^k * L) worst case (k = number of '.'), Space: O(total chars)
 */
public class Ques2_AddAndSearchWord {

    static class WordNode {
        WordNode[] children = new WordNode[26];
        boolean isEnd = false;
    }

    static class WordDictionary {
        private final WordNode root = new WordNode();

        public void addWord(String word) {
            WordNode node = root;
            for (int i = 0; i < word.length(); i++) {
                int idx = word.charAt(i) - 'a';
                if (node.children[idx] == null) {
                    node.children[idx] = new WordNode();
                }
                node = node.children[idx];
            }
            node.isEnd = true;
        }

        public boolean search(String word) {
            return match(root, word, 0);
        }

        private boolean match(WordNode node, String word, int i) {
            if (i == word.length()) {
                return node.isEnd; // must land on a complete word, not just a prefix
            }
            char c = word.charAt(i);
            if (c == '.') {
                for (WordNode child : node.children) { // branch into every existing child
                    if (child != null && match(child, word, i + 1)) {
                        return true;
                    }
                }
                return false;
            }
            WordNode child = node.children[c - 'a'];
            return child != null && match(child, word, i + 1);
        }
    }

    static void main() {
        WordDictionary d = new WordDictionary();
        d.addWord("bad");
        d.addWord("dad");
        d.addWord("mad");
        System.out.println(d.search("pad")); // false
        System.out.println(d.search("bad")); // true
        System.out.println(d.search(".ad")); // true
        System.out.println(d.search("b..")); // true
        System.out.println(d.search(".."));  // false
    }
}
