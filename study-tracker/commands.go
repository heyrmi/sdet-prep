package main

import (
	"bufio"
	"fmt"
	"os"
	"sort"
	"strings"
	"time"
)

// ---------- queue building (pure, testable) ----------

type queueOpts struct {
	max         int    // cap on total session size; <=0 = unlimited
	newOverride int    // -1 = use config/day-cap; else exact number of new to offer
	course      string // "", "sd", "dsa"
}

func courseMatch(it *Item, course string) bool {
	return course == "" || it.Course == course
}

// introducedToday counts items first reviewed today (to enforce the daily new cap).
func introducedToday(s *State, today time.Time) int {
	td := today.Format(dateFmt)
	n := 0
	for _, it := range s.Items {
		if len(it.History) > 0 && it.History[0].Date == td {
			n++
		}
	}
	return n
}

// buildQueue returns the due items and the new items to introduce this session.
func buildQueue(s *State, today time.Time, opts queueOpts) (due, news []*Item) {
	for _, it := range s.Items {
		if it.Suspended || !courseMatch(it, opts.course) {
			continue
		}
		if it.dueOn(today) {
			due = append(due, it)
		} else if it.IsNew() {
			news = append(news, it)
		}
	}
	// Due first by date (most overdue first), then by ID for stability.
	sort.Slice(due, func(i, j int) bool {
		if due[i].Due != due[j].Due {
			return due[i].Due < due[j].Due
		}
		return due[i].ID < due[j].ID
	})
	// New in natural course/module order.
	sort.Slice(news, func(i, j int) bool { return news[i].ID < news[j].ID })

	// Cap new items by the daily budget (unless overridden).
	newBudget := opts.newOverride
	if newBudget < 0 {
		newBudget = s.Config.NewPerDay - introducedToday(s, today)
	}
	if newBudget < 0 {
		newBudget = 0
	}
	if len(news) > newBudget {
		news = news[:newBudget]
	}
	return due, news
}

// ---------- commands ----------

func cmdInit(args []string) error {
	fs := newFlags(args)
	sd := fs.str("sd", "../sd")
	dsa := fs.str("dsa", "../dsa")
	sdet := fs.str("sdet", "../sdet")
	newPerDay := fs.int("new", -1)

	s, err := loadState()
	if err != nil {
		return err
	}
	rootSD, rootDSA, rootSDET := abs(sd), abs(dsa), abs(sdet)
	if !dirExists(rootSD) {
		return fmt.Errorf("system-design course not found at %s (pass --sd <path>)", rootSD)
	}
	if !dirExists(rootDSA) {
		return fmt.Errorf("DSA course not found at %s (pass --dsa <path>)", rootDSA)
	}
	s.Config.RootSD, s.Config.RootDSA = rootSD, rootDSA
	if dirExists(rootSDET) {
		s.Config.RootSDET = rootSDET // SDET module is optional
	} else {
		rootSDET = ""
	}
	if newPerDay >= 0 {
		s.Config.NewPerDay = newPerDay
	}

	added, updated := s.merge(scanCourses(rootSD, rootDSA, rootSDET))
	if err := s.save(); err != nil {
		return err
	}
	fmt.Printf("%s catalog refreshed: %s new item(s), %s already tracked.\n",
		green("✓"), bold(fmt.Sprint(added)), bold(fmt.Sprint(updated)))
	fmt.Printf("  SD:   %s\n  DSA:  %s\n", dim(rootSD), dim(rootDSA))
	if rootSDET != "" {
		fmt.Printf("  SDET: %s\n", dim(rootSDET))
	}
	fmt.Printf("  New items/day: %s   State: %s\n", bold(fmt.Sprint(s.Config.NewPerDay)), dim(statePath()))
	fmt.Printf("\nRun %s to start studying.\n", cyan("srs today"))
	return nil
}

func cmdStatus(args []string) error {
	s, err := loadState()
	if err != nil {
		return err
	}
	if len(s.Items) == 0 {
		fmt.Printf("No items yet. Run %s first (from the study-tracker directory).\n", cyan("srs init"))
		return nil
	}
	today := time.Now()

	// Tallies.
	stages := map[string]int{}
	perCourse := map[string]*[4]int{"sd": {}, "dsa": {}, "sdet": {}} // due, new, total, suspended
	for _, it := range s.Items {
		stages[it.stage()]++
		c := perCourse[it.Course]
		if c == nil {
			c = &[4]int{}
			perCourse[it.Course] = c
		}
		c[2]++
		if it.Suspended {
			c[3]++
			continue
		}
		if it.dueOn(today) {
			c[0]++
		} else if it.IsNew() {
			c[1]++
		}
	}
	due, news := buildQueue(s, today, queueOpts{newOverride: -1})

	fmt.Printf("\n  %s   %s\n", bold("📚 Study Tracker"), dim("("+today.Format("Mon 2 Jan 2006")+")"))
	streakStr := fmt.Sprintf("%d day", s.Streak)
	if s.Streak != 1 {
		streakStr += "s"
	}
	flame := ""
	if s.Streak > 0 {
		flame = "🔥 "
	}
	fmt.Printf("  %sStreak: %s\n\n", flame, bold(streakStr))

	fmt.Printf("  %s  %s due   %s new (of %d/day)\n",
		bold("Today:"), hot(fmt.Sprint(len(due))), cyan(fmt.Sprint(len(news))), s.Config.NewPerDay)
	fmt.Printf("  %s %d total · %s new · %s learning · %s young · %s mature\n\n",
		bold("Deck:"), len(s.Items), fmt.Sprint(stages["new"]),
		fmt.Sprint(stages["learning"]), fmt.Sprint(stages["young"]), green(fmt.Sprint(stages["mature"])))

	fmt.Printf("  %-6s %6s %6s %7s\n", "", "due", "new", "total")
	for _, c := range []string{"sd", "dsa", "sdet"} {
		v := perCourse[c]
		if v == nil {
			continue
		}
		fmt.Printf("  %-6s %6d %6d %7d\n", strings.ToUpper(c), v[0], v[1], v[2])
	}

	// 7-day forecast of scheduled reviews.
	fmt.Printf("\n  %s\n", dim("Next 7 days (scheduled reviews):"))
	for d := 1; d <= 7; d++ {
		day := today.AddDate(0, 0, d)
		ds := day.Format(dateFmt)
		n := 0
		for _, it := range s.Items {
			if !it.Suspended && it.Due == ds {
				n++
			}
		}
		bar := strings.Repeat("▇", min(n, 40))
		fmt.Printf("  %s  %3d %s\n", dim(day.Format("Mon 02")), n, dim(bar))
	}

	if len(due)+len(news) > 0 {
		fmt.Printf("\n  → Run %s to review %s item(s).\n\n", cyan("srs today"), bold(fmt.Sprint(len(due)+len(news))))
	} else {
		fmt.Printf("\n  %s All caught up for today. 🎉\n\n", green("✓"))
	}
	return nil
}

func recallPrompt(it *Item) string {
	if it.Type == "problem" {
		return "⌨  Re-solve it from scratch: cd into assignment/, write the code, run the tests.\n" +
			"   Then grade how it went."
	}
	return "🧠 Re-explain it out loud as if teaching a friend — the core idea, how it works,\n" +
		"   and the main trade-offs. Then open the lesson and check what you missed."
}

func cmdReview(args []string) error {
	fs := newFlags(args)
	opts := queueOpts{
		max:         fs.int("max", 0),
		newOverride: fs.int("new", -1),
		course:      fs.str("course", ""),
	}
	s, err := loadState()
	if err != nil {
		return err
	}
	if len(s.Items) == 0 {
		fmt.Printf("No items. Run %s first.\n", cyan("srs init"))
		return nil
	}
	today := time.Now()
	due, news := buildQueue(s, today, opts)
	queue := append(append([]*Item{}, due...), news...)
	if opts.max > 0 && len(queue) > opts.max {
		queue = queue[:opts.max]
	}
	if len(queue) == 0 {
		fmt.Printf("%s Nothing due. Enjoy the day off (or run %s to pull new items).\n",
			green("✓"), cyan("srs today --new 5"))
		return nil
	}

	fmt.Printf("\n%s  %d due + %d new = %s item(s). Grades: %s\n",
		bold("▶ Review session"), len(due), len(news), bold(fmt.Sprint(len(queue))),
		dim("[1]again [2]hard [3]good [4]easy  ·  s=skip  q=quit"))

	in := bufio.NewScanner(os.Stdin)
	reviewed, agains := 0, 0
	for i, it := range queue {
		fmt.Printf("\n%s\n", dim(strings.Repeat("─", 64)))
		tag := fmt.Sprintf("%s · %s · %s", strings.ToUpper(it.Course), it.Category, it.Type)
		if it.IsNew() {
			tag += "  " + cyan("[NEW]")
		}
		fmt.Printf("%s  %s\n", dim(fmt.Sprintf("[%d/%d]", i+1, len(queue))), dim(tag))
		fmt.Printf("  %s\n", bold(it.Title))
		fmt.Printf("  %s\n\n", dim(it.Path))
		fmt.Printf("  %s\n", recallPrompt(it))

	grade:
		for {
			fmt.Printf("\n  grade %s ", dim("(1/2/3/4 · s · q) ›"))
			if !in.Scan() {
				fmt.Println()
				_ = s.save()
				printSummary(reviewed, agains)
				return nil
			}
			cmd := strings.TrimSpace(strings.ToLower(in.Text()))
			switch cmd {
			case "q", "quit":
				_ = s.save()
				printSummary(reviewed, agains)
				return nil
			case "s", "skip", "":
				fmt.Printf("  %s skipped\n", dim("↷"))
				break grade
			default:
				g, ok := parseGrade(cmd)
				if !ok {
					fmt.Printf("  %s try 1/2/3/4, s, or q\n", yellow("?"))
					continue
				}
				ApplyGrade(it, g, today)
				s.recordStudy(today)
				if err := s.save(); err != nil {
					return err
				}
				reviewed++
				if g == Again {
					agains++
				}
				fmt.Printf("  %s next review in %s (due %s)\n",
					green("✓"), bold(humanDays(it.Interval)), it.Due)
				break grade
			}
		}
	}
	printSummary(reviewed, agains)
	return nil
}

func printSummary(reviewed, agains int) {
	fmt.Printf("\n%s\n", dim(strings.Repeat("─", 64)))
	fmt.Printf("%s reviewed %s · %s to relearn. Come back tomorrow. 👋\n\n",
		bold("Session done:"), bold(fmt.Sprint(reviewed)), fmt.Sprint(agains))
}

// cmdGrade grades a single item by ID (scriptable / non-interactive).
func cmdGrade(args []string) error {
	if len(args) < 2 {
		return fmt.Errorf("usage: srs grade <item-id> <again|hard|good|easy>")
	}
	id, gradeStr := args[0], args[1]
	g, ok := parseGrade(strings.ToLower(gradeStr))
	if !ok {
		return fmt.Errorf("unknown grade %q (use again|hard|good|easy)", gradeStr)
	}
	s, err := loadState()
	if err != nil {
		return err
	}
	it, ok := s.Items[id]
	if !ok {
		return fmt.Errorf("no item with id %q (try: srs list)", id)
	}
	today := time.Now()
	ApplyGrade(it, g, today)
	s.recordStudy(today)
	if err := s.save(); err != nil {
		return err
	}
	fmt.Printf("%s %s → %s, next review in %s (due %s)\n",
		green("✓"), it.Title, gradeStr, bold(humanDays(it.Interval)), it.Due)
	return nil
}

func cmdList(args []string) error {
	fs := newFlags(args)
	course := fs.str("course", "")
	filter := "all"
	for _, a := range args {
		switch a {
		case "--due":
			filter = "due"
		case "--new":
			filter = "new"
		case "--all":
			filter = "all"
		}
	}
	s, err := loadState()
	if err != nil {
		return err
	}
	today := time.Now()
	n := 0
	for _, it := range s.sortedItems() {
		if !courseMatch(it, course) {
			continue
		}
		switch filter {
		case "due":
			if !it.dueOn(today) {
				continue
			}
		case "new":
			if !it.IsNew() {
				continue
			}
		}
		due := it.Due
		if due == "" {
			due = "—"
		}
		fmt.Printf("  %-9s %-10s %-9s %s\n", stageLabel(it.stage()), due, dim(it.Course+"/"+it.Type), it.Title)
		fmt.Printf("  %s\n", dim("    "+it.ID))
		n++
	}
	fmt.Printf("\n  %d item(s).\n", n)
	return nil
}

func cmdNext(args []string) error {
	s, err := loadState()
	if err != nil {
		return err
	}
	due, news := buildQueue(s, time.Now(), queueOpts{newOverride: -1})
	queue := append(due, news...)
	if len(queue) == 0 {
		fmt.Printf("%s Nothing to study right now.\n", green("✓"))
		return nil
	}
	it := queue[0]
	fmt.Printf("%s\n%s\n%s\n\n%s\n", bold(it.Title), dim(it.ID), dim(it.Path), recallPrompt(it))
	fmt.Printf("\nGrade it with: %s\n", cyan("srs grade "+it.ID+" good"))
	return nil
}
