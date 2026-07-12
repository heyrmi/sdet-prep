// Command srs is a spaced-repetition study tracker for the System Design and DSA
// courses. It auto-discovers every lesson and problem, schedules reviews with an
// SM-2 (Anki-style) algorithm, and runs active-recall study sessions.
//
//	srs init            scan ../sd and ../dsa, build/refresh the deck
//	srs today           review everything due (plus a few new items)
//	srs status          dashboard: due, streak, forecast
//	srs grade <id> good grade one item without the interactive session
//	srs list [--due]    list items
//	srs next            show the single next thing to study
package main

import (
	"fmt"
	"math"
	"os"
	"strconv"
	"strings"
)

func main() {
	args := os.Args[1:]
	if len(args) == 0 {
		fail(cmdStatus(nil))
		return
	}
	cmd, rest := args[0], args[1:]
	switch cmd {
	case "init":
		fail(cmdInit(rest))
	case "today", "review":
		fail(cmdReview(rest))
	case "status", "stats":
		fail(cmdStatus(rest))
	case "grade":
		fail(cmdGrade(rest))
	case "list", "ls":
		fail(cmdList(rest))
	case "next":
		fail(cmdNext(rest))
	case "help", "-h", "--help":
		usage()
	default:
		fmt.Fprintf(os.Stderr, "%s unknown command %q\n\n", red("error:"), cmd)
		usage()
		os.Exit(2)
	}
}

func fail(err error) {
	if err != nil {
		fmt.Fprintf(os.Stderr, "%s %v\n", red("error:"), err)
		os.Exit(1)
	}
}

func usage() {
	fmt.Print(`srs — spaced-repetition tracker for the SD + DSA courses

USAGE
  srs init [--sd PATH] [--dsa PATH] [--new N]   build/refresh the deck (run once, re-run after adding content)
  srs today [--max N] [--new N] [--course sd|dsa]   review everything due + new items (interactive)
  srs status                                    dashboard: due count, streak, 7-day forecast
  srs next                                      print the single next item to study
  srs grade <item-id> <again|hard|good|easy>    grade one item non-interactively
  srs list [--all|--due|--new] [--course sd|dsa]  list items and their schedule
  srs help

GRADING (during 'srs today')
  1/again  blanked — resets the card, see it tomorrow
  2/hard   recalled with real difficulty — smaller interval
  3/good   solid — normal interval growth
  4/easy   trivial — bigger jump
  s skip · q quit (progress is saved after every grade)

State lives in ~/.srs/state.json (override with $SRS_STATE).
`)
}

// ---------- tiny flag parser (--key value or --key=value) ----------

type flagSet struct{ args []string }

func newFlags(args []string) *flagSet { return &flagSet{args} }

func (f *flagSet) str(name, def string) string {
	pre := "--" + name
	for i, a := range f.args {
		if a == pre && i+1 < len(f.args) {
			return f.args[i+1]
		}
		if strings.HasPrefix(a, pre+"=") {
			return a[len(pre)+1:]
		}
	}
	return def
}

func (f *flagSet) int(name string, def int) int {
	s := f.str(name, "")
	if s == "" {
		return def
	}
	if n, err := strconv.Atoi(s); err == nil {
		return n
	}
	return def
}

// ---------- formatting helpers ----------

var useColor = os.Getenv("NO_COLOR") == ""

func col(code, s string) string {
	if !useColor {
		return s
	}
	return "\x1b[" + code + "m" + s + "\x1b[0m"
}

func bold(s string) string   { return col("1", s) }
func dim(s string) string    { return col("2", s) }
func green(s string) string  { return col("32", s) }
func cyan(s string) string   { return col("36", s) }
func yellow(s string) string { return col("33", s) }
func red(s string) string    { return col("31", s) }
func hot(s string) string    { return col("1;31", s) }

func stageLabel(stage string) string {
	switch stage {
	case "new":
		return "new"
	case "learning":
		return "learning"
	case "young":
		return "young"
	default:
		return "mature"
	}
}

// humanDays renders an interval (in days) compactly: "1 day", "12 days", "≈3 months", "≈1.5 years".
func humanDays(d float64) string {
	n := int(math.Round(d))
	switch {
	case n <= 1:
		return "1 day"
	case n < 45:
		return fmt.Sprintf("%d days", n)
	case n < 365:
		return fmt.Sprintf("≈%d months", int(math.Round(float64(n)/30)))
	default:
		return fmt.Sprintf("≈%.1f years", float64(n)/365)
	}
}
