package main

import (
	"math"
	"time"
)

const (
	startEase = 2.5
	minEase   = 1.3
)

func clampEase(e float64) float64 {
	if e < minEase {
		return minEase
	}
	return e
}

// nextInterval implements the classic SM-2 ladder: the first two successful
// reviews use fixed steps (1 day, 6 days), after which the interval grows by a
// multiplier (the ease, or a hard/easy variant). `reps` is the count of prior
// successful reviews (before this one).
func nextInterval(reps int, prevInterval, mult float64) float64 {
	switch reps {
	case 0:
		return 1
	case 1:
		return 6
	default:
		return math.Round(prevInterval * mult)
	}
}

// ApplyGrade updates an item's SRS state given a grade and the current day.
// This is the heart of the spaced-repetition system (SM-2, Anki-flavored).
func ApplyGrade(it *Item, g Grade, today time.Time) {
	if it.Ease == 0 {
		it.Ease = startEase
	}

	switch g {
	case Again:
		// Blanked: reset progress, make it easier to trigger, see it again tomorrow.
		it.Lapses++
		it.Reps = 0
		it.Ease = clampEase(it.Ease - 0.20)
		it.Interval = 1
	case Hard:
		it.Ease = clampEase(it.Ease - 0.15)
		it.Interval = nextInterval(it.Reps, it.Interval, 1.2)
		it.Reps++
	case Good:
		it.Interval = nextInterval(it.Reps, it.Interval, it.Ease)
		it.Reps++
	case Easy:
		it.Ease = clampEase(it.Ease + 0.15)
		it.Interval = nextInterval(it.Reps, it.Interval, it.Ease*1.3)
		it.Reps++
	}

	if it.Interval < 1 {
		it.Interval = 1
	}
	it.Due = today.AddDate(0, 0, int(math.Round(it.Interval))).Format(dateFmt)
	it.LastReview = today.Format(dateFmt)
	it.History = append(it.History, Review{Date: today.Format(dateFmt), Grade: string(g), Interval: it.Interval})
}
