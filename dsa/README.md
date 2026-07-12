# DSA — 205 runnable problems, pattern-organized

Data-structures & algorithms for the **coding round**, in plain Java (no external deps). Every
problem is a single self-contained file with a `static void main()` that runs the solution and
prints results with the expected value in a trailing comment — run it and eyeball green/red.

```
dsa/
├── DSA_INTERVIEW_QUESTIONS.md   frequency-ranked strategy guide (MUST-DO vs GOOD-TO-KNOW, study plan)
├── lessons/                     orientation: Big-O, Java-for-interviews gotchas, UMPIRE, glossary
└── src/main/java/ra/hul/dsa/    205 problems across 28 topic/pattern packages
```

## Run any problem

```bash
mvn -pl dsa compile
mvn -pl dsa exec:java -Dexec.mainClass="ra.hul.dsa.binarysearch.Ques5_KokoEatingBananas"
# …or just click ▶ on the main() in your IDE (JDK 25)
```

## Coverage (205 problems, all ~15 canonical patterns)

**Data structures:** arrays (14), strings (9), hashmap (4), linkedlist (8), stack (5), queue (1),
tree (13), trie (5), heap (12), graph (14), unionfind (5), advancedtrees (4 — Fenwick/segment),
design (2 — LRU/LFU cache).

**Patterns:** twopointers (6), slidingwindow (9), fastslowpointers (5), prefixsum (7),
binarysearch (8), intervals (5), backtracking (11), dp (17), dpadvanced (8), greedy (8),
bitmanipulation (9), matrix (9), monotonicstack (5), sorting (1), recursion (1).

This spans Blind-75 and a large slice of NeetCode-150. See
[`DSA_INTERVIEW_QUESTIONS.md`](DSA_INTERVIEW_QUESTIONS.md) for the frequency ranking and an 8-week plan,
and [`lessons/`](lessons/) for the orientation material to read first.

## Convention

`Ques{N}_{ProblemName}.java`, package `ra.hul.dsa.<topic>`, solution logic in `static` methods,
helper types (`ListNode`, `TreeNode`, …) as nested `static` classes. The `main()` demonstrates the
examples with expected outputs inline.

> Spaced repetition: the [`../study-tracker/`](../study-tracker/) `srs` CLI indexes every problem here
> and schedules re-solves so they stick.
