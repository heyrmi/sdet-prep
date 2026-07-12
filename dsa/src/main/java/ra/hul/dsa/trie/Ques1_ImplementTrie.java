package ra.hul.dsa.trie;

/**
 * Implement Trie (Prefix Tree) - insert / search (full word) / startsWith (prefix). Every op O(L).
 * LeetCode #208 (Medium)
 *
 * Time: O(L) per op, Space: O(total characters inserted)
 */
public class Ques1_ImplementTrie {

    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEnd = false;
    }

    static class Trie {
        private final TrieNode root = new TrieNode();

        public void insert(String word) {
            TrieNode node = root;
            for (int i = 0; i < word.length(); i++) {
                int idx = word.charAt(i) - 'a';
                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }
                node = node.children[idx];
            }
            node.isEnd = true;
        }

        public boolean search(String word) {
            TrieNode node = walk(word);
            return node != null && node.isEnd; // must be a full word, not just a prefix
        }

        public boolean startsWith(String prefix) {
            return walk(prefix) != null; // arriving is enough; no isEnd check
        }

        /** Walk down following each char; return the landing node, or null if an edge is missing. */
        private TrieNode walk(String s) {
            TrieNode node = root;
            for (int i = 0; i < s.length(); i++) {
                int idx = s.charAt(i) - 'a';
                if (node.children[idx] == null) {
                    return null;
                }
                node = node.children[idx];
            }
            return node;
        }
    }

    static void main() {
        Trie t = new Trie();
        t.insert("apple");
        System.out.println(t.search("apple"));     // true
        System.out.println(t.search("app"));       // false
        System.out.println(t.startsWith("app"));   // true
        t.insert("app");
        System.out.println(t.search("app"));       // true
    }
}
