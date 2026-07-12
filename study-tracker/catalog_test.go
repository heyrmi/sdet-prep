package main

import (
	"os"
	"path/filepath"
	"testing"
)

func write(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

func TestScanCourses(t *testing.T) {
	sd := filepath.Join(t.TempDir(), "sd")
	dsa := filepath.Join(t.TempDir(), "dsa")

	write(t, filepath.Join(sd, "00-foundations", "01-intro.md"), "# 0.1 — Scale From Zero\n\nbody")
	write(t, filepath.Join(sd, "04-case-studies", "01-rate-limiter", "README.md"), "# 4.1 — Design a Rate Limiter\n")
	write(t, filepath.Join(dsa, "00-orientation", "01-big-o.md"), "# Big-O & Complexity\n")
	write(t, filepath.Join(dsa, "01-data-structures", "01-arrays-and-strings", "README.md"), "# 1.1 — Arrays & Strings\n")
	write(t, filepath.Join(dsa, "01-data-structures", "01-arrays-and-strings", "problems", "01-two-sum", "README.md"), "# Two Sum\n")
	if err := os.MkdirAll(filepath.Join(dsa, "01-data-structures", "01-arrays-and-strings", "problems", "01-two-sum", "assignment"), 0o755); err != nil {
		t.Fatal(err)
	}
	// A problems dir without assignment/ must be ignored.
	write(t, filepath.Join(dsa, "01-data-structures", "01-arrays-and-strings", "problems", "99-draft", "README.md"), "# Draft\n")

	items := scanCourses(sd, dsa, "")
	byID := map[string]*Item{}
	for _, it := range items {
		byID[it.ID] = it
	}

	cases := []struct {
		id, typ, title, course string
	}{
		{"sd/00-foundations/01-intro.md", "lesson", "Scale From Zero", "sd"},
		{"sd/04-case-studies/01-rate-limiter", "problem", "Design a Rate Limiter", "sd"},
		{"dsa/00-orientation/01-big-o.md", "lesson", "Big-O & Complexity", "dsa"},
		{"dsa/01-data-structures/01-arrays-and-strings/README.md", "lesson", "Arrays & Strings", "dsa"},
		{"dsa/01-data-structures/01-arrays-and-strings/problems/01-two-sum", "problem", "Two Sum", "dsa"},
	}
	for _, c := range cases {
		it, ok := byID[c.id]
		if !ok {
			t.Errorf("missing item %s", c.id)
			continue
		}
		if it.Type != c.typ || it.Title != c.title || it.Course != c.course {
			t.Errorf("item %s = {%s,%q,%s}, want {%s,%q,%s}", c.id, it.Type, it.Title, it.Course, c.typ, c.title, c.course)
		}
	}
	if _, ok := byID["dsa/01-data-structures/01-arrays-and-strings/problems/99-draft"]; ok {
		t.Error("problem dir without assignment/ should be ignored")
	}
}

func TestPrettifyAndCleanTitle(t *testing.T) {
	if got := prettify("02-building-blocks"); got != "Building Blocks" {
		t.Errorf("prettify = %q, want Building Blocks", got)
	}
	if got := prettify("07-koko-eating-bananas"); got != "Koko Eating Bananas" {
		t.Errorf("prettify = %q, want Koko Eating Bananas", got)
	}
	if got := cleanTitle("2.5 — Binary Search"); got != "Binary Search" {
		t.Errorf("cleanTitle = %q, want Binary Search", got)
	}
	if got := cleanTitle("Two Sum"); got != "Two Sum" {
		t.Errorf("cleanTitle = %q, want Two Sum", got)
	}
}

func TestBuildQueue(t *testing.T) {
	s := newState()
	yesterday := testToday.AddDate(0, 0, -1).Format(dateFmt)
	tomorrow := testToday.AddDate(0, 0, 1).Format(dateFmt)
	s.Items = map[string]*Item{
		"due":    {ID: "due", Course: "dsa", Due: yesterday, Interval: 3, Reps: 2},
		"new1":   {ID: "new1", Course: "dsa"},
		"new2":   {ID: "new2", Course: "sd"},
		"future": {ID: "future", Course: "sd", Due: tomorrow, Interval: 5, Reps: 2},
	}
	due, news := buildQueue(s, testToday, queueOpts{newOverride: -1})
	if len(due) != 1 || due[0].ID != "due" {
		t.Fatalf("due = %v, want [due]", ids(due))
	}
	if len(news) != 2 {
		t.Fatalf("news = %v, want 2 new items", ids(news))
	}

	// course filter
	due, news = buildQueue(s, testToday, queueOpts{newOverride: -1, course: "sd"})
	if len(due) != 0 || len(news) != 1 || news[0].ID != "new2" {
		t.Fatalf("course=sd filter wrong: due=%v news=%v", ids(due), ids(news))
	}
}

func TestNewDailyCap(t *testing.T) {
	s := newState()
	s.Config.NewPerDay = 2
	today := testToday.Format(dateFmt)
	s.Items = map[string]*Item{
		// one item already introduced today eats into the budget
		"introduced": {ID: "introduced", Course: "dsa", Due: testToday.AddDate(0, 0, 6).Format(dateFmt),
			Interval: 6, Reps: 2, History: []Review{{Date: today, Grade: "good"}}},
		"new1": {ID: "new1", Course: "dsa"},
		"new2": {ID: "new2", Course: "dsa"},
		"new3": {ID: "new3", Course: "dsa"},
	}
	_, news := buildQueue(s, testToday, queueOpts{newOverride: -1})
	if len(news) != 1 {
		t.Fatalf("with cap 2 and 1 already introduced today, want 1 new, got %d (%v)", len(news), ids(news))
	}

	// explicit override ignores the daily cap
	_, news = buildQueue(s, testToday, queueOpts{newOverride: 3})
	if len(news) != 3 {
		t.Fatalf("override 3 should yield 3 new, got %d", len(news))
	}
}

func ids(items []*Item) []string {
	out := make([]string, len(items))
	for i, it := range items {
		out[i] = it.ID
	}
	return out
}
