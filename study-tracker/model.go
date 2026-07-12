package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"time"
)

const dateFmt = "2006-01-02"

// Grade is the self-assessment after reviewing an item (Anki-style).
type Grade string

const (
	Again Grade = "again" // failed / blanked — reset
	Hard  Grade = "hard"  // recalled with serious difficulty
	Good  Grade = "good"  // recalled correctly
	Easy  Grade = "easy"  // trivial — push the interval out further
)

func parseGrade(s string) (Grade, bool) {
	switch s {
	case "again", "a", "1":
		return Again, true
	case "hard", "h", "2":
		return Hard, true
	case "good", "g", "3":
		return Good, true
	case "easy", "e", "4":
		return Easy, true
	}
	return "", false
}

// Review is one logged study event for an item.
type Review struct {
	Date     string  `json:"date"`
	Grade    string  `json:"grade"`
	Interval float64 `json:"interval"`
}

// Item is one trackable unit of study: a lesson to re-explain or a problem to re-solve.
type Item struct {
	ID       string `json:"id"`       // stable, e.g. "dsa/02-patterns/05-binary-search/problems/07-koko-eating-bananas"
	Course   string `json:"course"`   // "sd" | "dsa"
	Type     string `json:"type"`     // "lesson" | "problem"
	Title    string `json:"title"`    // human title from the file's H1
	Path     string `json:"path"`     // absolute path to the README/lesson to open
	Category string `json:"category"` // module/pattern name

	// SRS state
	Ease       float64  `json:"ease"`          // 1.3..~3.0, starts 2.5
	Interval   float64  `json:"interval_days"` // current interval in days
	Reps       int      `json:"reps"`          // consecutive successful reviews
	Lapses     int      `json:"lapses"`        // times graded "again"
	Due        string   `json:"due,omitempty"` // YYYY-MM-DD; "" means new (not yet introduced)
	LastReview string   `json:"last_review,omitempty"`
	Suspended  bool     `json:"suspended,omitempty"`
	History    []Review `json:"history,omitempty"`
}

// IsNew reports whether the item has never been scheduled.
func (it *Item) IsNew() bool { return it.Due == "" }

// stage classifies an item for the dashboard.
func (it *Item) stage() string {
	switch {
	case it.IsNew():
		return "new"
	case it.Interval < 6:
		return "learning"
	case it.Interval < 21:
		return "young"
	default:
		return "mature"
	}
}

// dueOn reports whether the item is due for review on or before `today`.
func (it *Item) dueOn(today time.Time) bool {
	if it.Suspended || it.IsNew() {
		return false
	}
	d, err := time.Parse(dateFmt, it.Due)
	if err != nil {
		return true // malformed → surface it rather than hide it
	}
	return !d.After(today)
}

// Config holds where the courses live.
type Config struct {
	RootSD    string `json:"root_sd"`
	RootDSA   string `json:"root_dsa"`
	RootSDET  string `json:"root_sdet,omitempty"`
	NewPerDay int    `json:"new_per_day"` // cap on freshly-introduced items per study day
}

// State is the whole persisted world.
type State struct {
	Version       int              `json:"version"`
	Config        Config           `json:"config"`
	Streak        int              `json:"streak"`
	LastStudyDate string           `json:"last_study_date,omitempty"`
	Items         map[string]*Item `json:"items"`
}

func newState() *State {
	return &State{Version: 1, Config: Config{NewPerDay: 8}, Items: map[string]*Item{}}
}

// statePath resolves where state.json lives (override with SRS_STATE).
func statePath() string {
	if p := os.Getenv("SRS_STATE"); p != "" {
		return p
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return ".srs_state.json"
	}
	return filepath.Join(home, ".srs", "state.json")
}

func loadState() (*State, error) {
	p := statePath()
	b, err := os.ReadFile(p)
	if os.IsNotExist(err) {
		return newState(), nil
	}
	if err != nil {
		return nil, err
	}
	var s State
	if err := json.Unmarshal(b, &s); err != nil {
		return nil, fmt.Errorf("state file %s is corrupt: %w", p, err)
	}
	if s.Items == nil {
		s.Items = map[string]*Item{}
	}
	if s.Config.NewPerDay == 0 {
		s.Config.NewPerDay = 8
	}
	return &s, nil
}

func (s *State) save() error {
	p := statePath()
	if err := os.MkdirAll(filepath.Dir(p), 0o755); err != nil {
		return err
	}
	b, err := json.MarshalIndent(s, "", "  ")
	if err != nil {
		return err
	}
	tmp := p + ".tmp"
	if err := os.WriteFile(tmp, b, 0o644); err != nil {
		return err
	}
	return os.Rename(tmp, p) // atomic replace
}

// merge folds freshly-scanned items into the state, preserving SRS progress for
// items that already exist and adding new ones as "new".
func (s *State) merge(found []*Item) (added, updated int) {
	for _, f := range found {
		if ex, ok := s.Items[f.ID]; ok {
			ex.Title, ex.Path, ex.Category, ex.Course, ex.Type = f.Title, f.Path, f.Category, f.Course, f.Type
			updated++
		} else {
			s.Items[f.ID] = f
			added++
		}
	}
	return
}

// recordStudy bumps the daily streak the first time you study on a given day.
func (s *State) recordStudy(today time.Time) {
	td := today.Format(dateFmt)
	if s.LastStudyDate == td {
		return
	}
	if s.LastStudyDate == today.AddDate(0, 0, -1).Format(dateFmt) {
		s.Streak++
	} else {
		s.Streak = 1
	}
	s.LastStudyDate = td
}

// sortedItems returns items in a stable display order.
func (s *State) sortedItems() []*Item {
	out := make([]*Item, 0, len(s.Items))
	for _, it := range s.Items {
		out = append(out, it)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].ID < out[j].ID })
	return out
}
