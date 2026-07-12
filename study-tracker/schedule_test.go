package main

import (
	"testing"
	"time"
)

var testToday = time.Date(2026, 6, 18, 0, 0, 0, 0, time.UTC)

func TestSM2Ladder(t *testing.T) {
	it := &Item{}

	ApplyGrade(it, Good, testToday) // 1st success
	if it.Interval != 1 || it.Reps != 1 {
		t.Fatalf("after 1st good: interval=%v reps=%d, want 1/1", it.Interval, it.Reps)
	}
	if it.Due != "2026-06-19" {
		t.Fatalf("due=%s, want 2026-06-19 (today+1)", it.Due)
	}

	ApplyGrade(it, Good, testToday) // 2nd success
	if it.Interval != 6 || it.Reps != 2 {
		t.Fatalf("after 2nd good: interval=%v reps=%d, want 6/2", it.Interval, it.Reps)
	}

	ApplyGrade(it, Good, testToday) // 3rd success: 6 * ease(2.5) = 15
	if it.Interval != 15 {
		t.Fatalf("after 3rd good: interval=%v, want 15", it.Interval)
	}
}

func TestAgainResets(t *testing.T) {
	it := &Item{Ease: 2.5, Interval: 30, Reps: 4}
	ApplyGrade(it, Again, testToday)
	if it.Reps != 0 {
		t.Errorf("again should reset reps to 0, got %d", it.Reps)
	}
	if it.Lapses != 1 {
		t.Errorf("again should increment lapses, got %d", it.Lapses)
	}
	if it.Interval != 1 {
		t.Errorf("again should set interval to 1, got %v", it.Interval)
	}
	if it.Ease < 2.29 || it.Ease > 2.31 {
		t.Errorf("again should drop ease by 0.20 (2.5→2.3), got %v", it.Ease)
	}
	if it.Due != "2026-06-19" {
		t.Errorf("again should reschedule for tomorrow, got %s", it.Due)
	}
}

func TestEaseAdjustments(t *testing.T) {
	hard := &Item{Ease: 2.5, Reps: 3, Interval: 10}
	ApplyGrade(hard, Hard, testToday)
	if hard.Ease > 2.36 || hard.Ease < 2.34 { // 2.5 - 0.15
		t.Errorf("hard ease = %v, want ~2.35", hard.Ease)
	}

	easy := &Item{Ease: 2.5, Reps: 3, Interval: 10}
	ApplyGrade(easy, Easy, testToday)
	if easy.Ease < 2.64 || easy.Ease > 2.66 { // 2.5 + 0.15
		t.Errorf("easy ease = %v, want ~2.65", easy.Ease)
	}
	// easy interval = round(10 * ease(2.65) * 1.3) = round(34.45) = 34
	if easy.Interval != 34 {
		t.Errorf("easy interval = %v, want 34", easy.Interval)
	}
}

func TestEaseFloor(t *testing.T) {
	it := &Item{Ease: startEase}
	for i := 0; i < 12; i++ {
		ApplyGrade(it, Again, testToday)
	}
	if it.Ease < minEase-1e-9 || it.Ease > minEase+1e-9 {
		t.Errorf("ease should clamp at %v, got %v", minEase, it.Ease)
	}
}

func TestHardMultiplierAfterGraduation(t *testing.T) {
	// reps>=2 so hard uses the 1.2 multiplier on the prior interval.
	it := &Item{Ease: 2.5, Reps: 2, Interval: 20}
	ApplyGrade(it, Hard, testToday)
	if it.Interval != 24 { // round(20 * 1.2)
		t.Errorf("hard interval = %v, want 24", it.Interval)
	}
}

func TestHistoryAppended(t *testing.T) {
	it := &Item{}
	ApplyGrade(it, Good, testToday)
	ApplyGrade(it, Easy, testToday)
	if len(it.History) != 2 {
		t.Fatalf("history len = %d, want 2", len(it.History))
	}
	if it.History[1].Grade != "easy" {
		t.Errorf("last history grade = %s, want easy", it.History[1].Grade)
	}
}
