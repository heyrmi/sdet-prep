// Package autocomplete is the reference solution for Module 4.9.
// Try the assignment yourself before reading this!
package autocomplete

import "sort"

type node struct {
	children map[rune]*node
	isWord   bool
	freq     int
}

func newNode() *node {
	return &node{children: make(map[rune]*node)}
}

type Trie struct {
	root *node
}

func NewTrie() *Trie {
	return &Trie{root: newNode()}
}

// walk descends to the node for word, creating nodes along the way.
func (t *Trie) walk(word string) *node {
	cur := t.root
	for _, r := range word {
		next := cur.children[r]
		if next == nil {
			next = newNode()
			cur.children[r] = next
		}
		cur = next
	}
	return cur
}

func (t *Trie) Insert(word string, freq int) {
	if word == "" {
		return
	}
	n := t.walk(word)
	n.isWord = true
	n.freq = freq
}

func (t *Trie) Bump(word string) {
	if word == "" {
		return
	}
	n := t.walk(word)
	n.isWord = true
	n.freq++
}

type entry struct {
	word string
	freq int
}

func (t *Trie) Suggest(prefix string, k int) []string {
	if k <= 0 {
		return []string{}
	}

	// Walk to the prefix node; bail if the path is missing.
	cur := t.root
	for _, r := range prefix {
		next := cur.children[r]
		if next == nil {
			return []string{}
		}
		cur = next
	}

	// Collect all complete words beneath (and including) the prefix node.
	var found []entry
	var dfs func(n *node, word string)
	dfs = func(n *node, word string) {
		if n.isWord {
			found = append(found, entry{word: word, freq: n.freq})
		}
		for r, child := range n.children {
			dfs(child, word+string(r))
		}
	}
	dfs(cur, prefix)

	// Rank: frequency DESC, then word ASC.
	sort.Slice(found, func(i, j int) bool {
		if found[i].freq != found[j].freq {
			return found[i].freq > found[j].freq
		}
		return found[i].word < found[j].word
	})

	if len(found) > k {
		found = found[:k]
	}
	out := make([]string, len(found))
	for i, e := range found {
		out[i] = e.word
	}
	return out
}
