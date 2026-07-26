package main

import (
	"bufio"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

// scanCourses discovers all trackable items from the SD, DSA and SDET trees.
func scanCourses(rootSD, rootDSA, rootSDET string) []*Item {
	var items []*Item
	if rootSD != "" && dirExists(rootSD) {
		items = append(items, scanSD(rootSD)...)
	}
	if rootDSA != "" && dirExists(rootDSA) {
		items = append(items, scanDSA(rootDSA)...)
	}
	if rootSDET != "" && dirExists(rootSDET) {
		items = append(items, scanSDET(rootSDET)...)
	}
	sort.Slice(items, func(i, j int) bool { return items[i].ID < items[j].ID })
	return items
}

// --- System Design ---
//
// Lessons are the individual .md files inside modules 00-03; case studies are
// each NN-name/README.md under 04-case-studies (they ship Go assignments, so we
// treat them as "problem" items you re-solve).
func scanSD(root string) []*Item {
	var items []*Item
	// Every module whose .md files are lessons. 04-case-studies is handled separately
	// below because its content lives one directory deeper.
	//
	// Keep this list in sync when a module is added — a missing entry is silent, and the
	// lessons simply never enter the review deck.
	lessonModules := []string{
		"00-foundations",
		"01-networking-and-communication",
		"02-building-blocks",
		"03-distributed-systems",
		"05-sdet-system-design",
		"06-ai-system-design",
		"07-testing-distributed-systems",
	}
	for _, m := range lessonModules {
		dir := filepath.Join(root, m)
		entries, _ := os.ReadDir(dir)
		for _, e := range entries {
			if e.IsDir() || !strings.HasSuffix(e.Name(), ".md") {
				continue
			}
			p := filepath.Join(dir, e.Name())
			items = append(items, &Item{
				ID:       "sd/" + m + "/" + e.Name(),
				Course:   "sd",
				Type:     "lesson",
				Title:    titleOf(p, e.Name()),
				Path:     abs(p),
				Category: prettify(m),
			})
		}
	}
	// Case studies
	csDir := filepath.Join(root, "04-case-studies")
	cs, _ := os.ReadDir(csDir)
	for _, e := range cs {
		if !e.IsDir() {
			continue
		}
		readme := filepath.Join(csDir, e.Name(), "README.md")
		if !fileExists(readme) {
			continue
		}
		items = append(items, &Item{
			ID:       "sd/04-case-studies/" + e.Name(),
			Course:   "sd",
			Type:     "problem",
			Title:    titleOf(readme, e.Name()),
			Path:     abs(readme),
			Category: "Case Studies",
		})
	}
	return items
}

// --- DSA ---
//
// Supports two layouts at once:
//   - Course-style: orientation/*.md, each module's README.md, and every
//     */problems/NN-name/ dir that contains an assignment/.
//   - Maven-style (this repo): runnable problems at
//     src/main/java/ra/hul/dsa/<topic>/Ques*.java, orientation notes under
//     lessons/00-orientation/, and the DSA_INTERVIEW_QUESTIONS.md guide.
func scanDSA(root string) []*Item {
	var items []*Item

	// Orientation lessons (flat .md files) — course-style 00-orientation/ or
	// the Maven repo's lessons/00-orientation/.
	for _, orient := range []string{
		filepath.Join(root, "00-orientation"),
		filepath.Join(root, "lessons", "00-orientation"),
	} {
		oe, _ := os.ReadDir(orient)
		for _, e := range oe {
			if e.IsDir() || !strings.HasSuffix(e.Name(), ".md") {
				continue
			}
			p := filepath.Join(orient, e.Name())
			items = append(items, &Item{
				ID: "dsa/00-orientation/" + e.Name(), Course: "dsa", Type: "lesson",
				Title: titleOf(p, e.Name()), Path: abs(p), Category: "Orientation",
			})
		}
	}

	// Maven-style runnable problems (this repo's primary DSA layout).
	items = append(items, scanMavenJava(
		filepath.Join(root, "src", "main", "java", "ra", "hul", "dsa"), "dsa")...)

	// Top-level DSA study guides, tracked as lessons to revisit.
	for _, g := range []struct{ file, cat string }{
		{"DSA_INTERVIEW_QUESTIONS.md", "Study Guide"},
		{filepath.Join("lessons", "GLOSSARY.md"), "Glossary"},
	} {
		p := filepath.Join(root, g.file)
		if fileExists(p) {
			items = append(items, &Item{
				ID: "dsa/guide/" + strings.ReplaceAll(g.file, string(filepath.Separator), "/"),
				Course: "dsa", Type: "lesson",
				Title: titleOf(p, g.file), Path: abs(p), Category: g.cat,
			})
		}
	}

	// Module overview lessons + their problems
	for _, part := range []string{"01-data-structures", "02-patterns"} {
		partDir := filepath.Join(root, part)
		mods, _ := os.ReadDir(partDir)
		for _, m := range mods {
			if !m.IsDir() {
				continue
			}
			modDir := filepath.Join(partDir, m.Name())
			cat := prettify(m.Name())

			// module overview lesson
			if readme := filepath.Join(modDir, "README.md"); fileExists(readme) {
				items = append(items, &Item{
					ID: "dsa/" + part + "/" + m.Name() + "/README.md", Course: "dsa", Type: "lesson",
					Title: titleOf(readme, m.Name()), Path: abs(readme), Category: cat,
				})
			}

			// problems
			probDir := filepath.Join(modDir, "problems")
			probs, _ := os.ReadDir(probDir)
			for _, p := range probs {
				if !p.IsDir() {
					continue
				}
				pd := filepath.Join(probDir, p.Name())
				if !dirExists(filepath.Join(pd, "assignment")) {
					continue
				}
				readme := filepath.Join(pd, "README.md")
				items = append(items, &Item{
					ID:     "dsa/" + part + "/" + m.Name() + "/problems/" + p.Name(),
					Course: "dsa", Type: "problem",
					Title:    titleOf(readme, p.Name()),
					Path:     abs(pd),
					Category: cat,
				})
			}
		}
	}
	return items
}

// --- SDET (practical automation / test-engineering) ---
//
// Problems: runnable Ques*.java under src/main/java/ra/hul/sdet/<topic>/.
// Lessons: the SDET question guide and company-round deep dives worth re-explaining.
func scanSDET(root string) []*Item {
	var items []*Item

	items = append(items, scanMavenJava(
		filepath.Join(root, "src", "main", "java", "ra", "hul", "sdet"), "sdet")...)

	for _, g := range []struct{ file, cat string }{
		{"SDET_INTERVIEW_QUESTIONS.md", "Study Guide"},
		{filepath.Join("company-questions", "jiostar-hotstar-framework-round.md"), "Company Deep-Dive"},
	} {
		p := filepath.Join(root, g.file)
		if fileExists(p) {
			items = append(items, &Item{
				ID: "sdet/guide/" + strings.ReplaceAll(g.file, string(filepath.Separator), "/"),
				Course: "sdet", Type: "lesson",
				Title: titleOf(p, g.file), Path: abs(p), Category: g.cat,
			})
		}
	}
	return items
}

// scanMavenJava turns a ra/hul/<course> package tree into problem items — one per
// Ques*.java file, categorised by its topic (sub-package) directory. Returns
// nothing if the tree is absent, so it's safe to call on either layout.
func scanMavenJava(pkgRoot, course string) []*Item {
	var items []*Item
	topics, _ := os.ReadDir(pkgRoot)
	for _, t := range topics {
		if !t.IsDir() {
			continue
		}
		topicDir := filepath.Join(pkgRoot, t.Name())
		cat := prettify(t.Name())
		files, _ := os.ReadDir(topicDir)
		for _, f := range files {
			name := f.Name()
			if f.IsDir() || !strings.HasPrefix(name, "Ques") || !strings.HasSuffix(name, ".java") {
				continue
			}
			base := strings.TrimSuffix(name, ".java")
			items = append(items, &Item{
				ID:       course + "/" + t.Name() + "/" + base,
				Course:   course,
				Type:     "problem",
				Title:    humanizeClass(base),
				Path:     abs(filepath.Join(topicDir, name)),
				Category: cat,
			})
		}
	}
	return items
}

// humanizeClass turns "Ques12_FindMinimumInRotatedSortedArray" into
// "Find Minimum In Rotated Sorted Array" (strip the QuesN_ prefix, split camelCase).
func humanizeClass(base string) string {
	s := base
	if strings.HasPrefix(s, "Ques") {
		if i := strings.Index(s, "_"); i > 0 {
			s = s[i+1:]
		}
	}
	var b strings.Builder
	for i, r := range s {
		if i > 0 && r >= 'A' && r <= 'Z' {
			b.WriteByte(' ')
		}
		b.WriteRune(r)
	}
	out := strings.TrimSpace(b.String())
	if out == "" {
		return base
	}
	return out
}

// titleOf reads the first markdown H1 ("# Title") from a file; falls back to a
// prettified version of the supplied default (a file/dir name).
func titleOf(path, fallback string) string {
	f, err := os.Open(path)
	if err == nil {
		defer f.Close()
		sc := bufio.NewScanner(f)
		sc.Buffer(make([]byte, 1024*1024), 1024*1024)
		for sc.Scan() {
			line := strings.TrimSpace(sc.Text())
			if strings.HasPrefix(line, "# ") {
				return cleanTitle(strings.TrimSpace(line[2:]))
			}
		}
	}
	return prettify(strings.TrimSuffix(fallback, ".md"))
}

// cleanTitle strips a leading "N.N — " or "4.1 — " numbering prefix for compactness.
func cleanTitle(t string) string {
	for _, sep := range []string{" — ", " - ", ": "} {
		if i := strings.Index(t, sep); i > 0 && i <= 8 {
			return strings.TrimSpace(t[i+len(sep):])
		}
	}
	return t
}

// prettify turns "02-building-blocks" or "07-koko-eating-bananas" into "Building Blocks".
func prettify(name string) string {
	name = strings.TrimSuffix(name, ".md")
	parts := strings.Split(name, "-")
	var words []string
	for _, p := range parts {
		if p == "" || isAllDigits(p) {
			continue // drop the leading NN- ordering prefix
		}
		words = append(words, strings.ToUpper(p[:1])+p[1:])
	}
	if len(words) == 0 {
		return name
	}
	return strings.Join(words, " ")
}

func isAllDigits(s string) bool {
	for _, r := range s {
		if r < '0' || r > '9' {
			return false
		}
	}
	return s != ""
}

func abs(p string) string {
	if a, err := filepath.Abs(p); err == nil {
		return a
	}
	return p
}

func fileExists(p string) bool {
	fi, err := os.Stat(p)
	return err == nil && !fi.IsDir()
}

func dirExists(p string) bool {
	fi, err := os.Stat(p)
	return err == nil && fi.IsDir()
}
