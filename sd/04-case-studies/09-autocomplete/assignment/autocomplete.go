// Package autocomplete is the Module 4.9 assignment: a prefix-search Trie with
// per-word popularity (frequency), the core data structure behind search
// typeahead / autocomplete.
//
// Read 04-case-studies/09-autocomplete/README.md first.
//
// Fill in every function marked `// TODO`. Run the tests until green:
//
//	go test ./...
//
// The Trie stores words character-by-character down a tree. A node that ends a
// word carries that word's frequency (how popular it is). Suggest walks to the
// node for a prefix, collects every complete word beneath it, and returns the
// top-k ranked by frequency DESC, breaking ties lexicographically ASC.
package autocomplete

// node is one character position in the Trie.
type node struct {
	children map[rune]*node
	isWord   bool
	freq     int // meaningful only when isWord is true
}

func newNode() *node {
	return &node{children: make(map[rune]*node)}
}

// Trie is a prefix tree of words with frequencies. The zero value is not ready;
// use NewTrie.
type Trie struct {
	root *node
}

// NewTrie returns an empty Trie.
func NewTrie() *Trie {
	return &Trie{root: newNode()}
}

// Insert adds word with an explicit frequency, SETTING (overwriting) the freq if
// the word already exists. Inserting an empty string is a no-op.
func (t *Trie) Insert(word string, freq int) {
	// TODO:
	//  1. If word == "", return.
	//  2. Walk from t.root, one rune at a time, creating child nodes as needed.
	//  3. At the final node, mark isWord = true and set freq = freq.
	panic("TODO: implement Trie.Insert")
}

// Bump increments word's frequency by 1. If the word is not present yet, it is
// inserted with frequency 1. Bumping an empty string is a no-op.
func (t *Trie) Bump(word string) {
	// TODO:
	//  1. If word == "", return.
	//  2. Walk/create nodes down to the final node for word.
	//  3. Mark isWord = true and increment freq by 1.
	panic("TODO: implement Trie.Bump")
}

// Suggest returns up to k complete words that start with prefix, ranked by
// frequency DESC, then lexicographically ASC for ties. A prefix that is itself a
// stored word is included in its own results. If prefix matches no node, or k <= 0,
// it returns an empty (len 0) slice. An empty prefix matches all words.
func (t *Trie) Suggest(prefix string, k int) []string {
	// TODO:
	//  1. If k <= 0, return an empty slice.
	//  2. Walk from t.root following prefix; if any rune is missing, return empty.
	//  3. From the node you landed on, DFS to collect every (word, freq) where
	//     isWord is true (remember to include the prefix node itself if it is a word).
	//  4. Sort: freq DESC, then word ASC. Return the first k words.
	panic("TODO: implement Trie.Suggest")
}
