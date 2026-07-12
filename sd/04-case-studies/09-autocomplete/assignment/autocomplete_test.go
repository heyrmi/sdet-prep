package autocomplete

import (
	"reflect"
	"testing"
)

func build(words map[string]int) *Trie {
	t := NewTrie()
	for w, f := range words {
		t.Insert(w, f)
	}
	return t
}

func TestRankedByFrequencyThenLex(t *testing.T) {
	tr := build(map[string]int{
		"cat":  10,
		"car":  10, // tie with cat -> lexicographic: car before cat
		"care": 5,
		"cart": 50,
	})
	got := tr.Suggest("ca", 10)
	want := []string{"cart", "car", "cat", "care"} // 50, then 10/10 (car<cat), then 5
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("ranking wrong:\n got  %v\n want %v", got, want)
	}
}

func TestPrefixFilters(t *testing.T) {
	tr := build(map[string]int{
		"apple":  3,
		"apply":  2,
		"banana": 9,
		"app":    1,
	})
	got := tr.Suggest("app", 10)
	want := []string{"apple", "apply", "app"} // banana excluded; 3,2,1
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("prefix filter wrong:\n got  %v\n want %v", got, want)
	}
}

func TestKLimitsResults(t *testing.T) {
	tr := build(map[string]int{
		"go":     100,
		"golang": 90,
		"gopher": 80,
		"good":   70,
	})
	got := tr.Suggest("go", 2)
	want := []string{"go", "golang"}
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("k limit wrong:\n got  %v\n want %v", got, want)
	}
}

func TestEmptyAndNoMatch(t *testing.T) {
	tr := build(map[string]int{"hello": 5})

	if got := tr.Suggest("xyz", 5); len(got) != 0 {
		t.Fatalf("no-match prefix should return empty, got %v", got)
	}
	if got := tr.Suggest("hello", 0); len(got) != 0 {
		t.Fatalf("k=0 should return empty, got %v", got)
	}
	empty := NewTrie()
	if got := empty.Suggest("a", 5); len(got) != 0 {
		t.Fatalf("empty trie should return empty, got %v", got)
	}
}

func TestEmptyPrefixMatchesAll(t *testing.T) {
	tr := build(map[string]int{"a": 1, "b": 3, "c": 2})
	got := tr.Suggest("", 10)
	want := []string{"b", "c", "a"} // 3, 2, 1
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("empty prefix should match all:\n got  %v\n want %v", got, want)
	}
}

func TestBumpChangesRanking(t *testing.T) {
	tr := build(map[string]int{"red": 5, "reef": 3})

	if got := tr.Suggest("re", 2); !reflect.DeepEqual(got, []string{"red", "reef"}) {
		t.Fatalf("before bump expected [red reef], got %v", got)
	}
	// Bump reef past red.
	for i := 0; i < 3; i++ {
		tr.Bump("reef") // 3 -> 6
	}
	if got := tr.Suggest("re", 2); !reflect.DeepEqual(got, []string{"reef", "red"}) {
		t.Fatalf("after bump expected [reef red], got %v", got)
	}

	// Bump of a brand-new word inserts it with freq 1.
	tr.Bump("rest")
	got := tr.Suggest("rest", 5)
	if !reflect.DeepEqual(got, []string{"rest"}) {
		t.Fatalf("bump of new word should insert it, got %v", got)
	}
}

func TestPrefixThatIsItselfAWord(t *testing.T) {
	tr := build(map[string]int{
		"in":     7,
		"inn":    4,
		"inside": 9,
	})
	got := tr.Suggest("in", 10)
	want := []string{"inside", "in", "inn"} // 9, 7, 4 — "in" itself included
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("prefix-is-a-word wrong:\n got  %v\n want %v", got, want)
	}
}

func TestInsertOverwritesFreq(t *testing.T) {
	tr := NewTrie()
	tr.Insert("x", 1)
	tr.Insert("x", 99) // SET, not add
	tr.Insert("y", 50)
	got := tr.Suggest("", 2)
	want := []string{"x", "y"} // 99 > 50
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("Insert should overwrite freq:\n got  %v\n want %v", got, want)
	}
}
