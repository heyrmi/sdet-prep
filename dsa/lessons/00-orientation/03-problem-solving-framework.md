# 0.3 — A Problem-Solving Framework (UMPIRE)

> **Part 0 · Orientation** · ~28 min read
> *The candidates who pass aren't the ones who instantly know the answer — they're the ones with a
> repeatable process that turns a blank screen into a clear plan, out loud. This is that process.*

---

## The problem

You're handed a problem you've never seen. The clock is running, someone's watching, and the
silence is loud. The instinct is to start typing the first idea — and that's exactly how good
candidates fail: they code a brute force into a corner, can't explain their plan, miss an edge
case, and run out of time. The fix isn't being smarter. It's having a **process** you trust so much
that the pressure can't knock you off it.

> **Analogy.** A pilot doesn't "wing it" even after 10,000 hours — they run a **checklist** every
> single flight, out loud, because checklists catch the thing your confidence skips. UMPIRE is your
> pre-flight checklist for coding interviews. Boring on easy problems, life-saving on hard ones.

UMPIRE is six steps: **U**nderstand, **M**atch, **P**lan, **I**mplement, **R**eview, **E**valuate.

```
U → M → P → I → R → E
Understand  Match   Plan    Implement  Review  Evaluate
the problem a pattern pseudocode  the code  trace   complexity
```

---

## U — Understand

**What to do:** Restate the problem in your own words. Nail down inputs, outputs, and constraints.
Surface ambiguities *before* you design — the wrong assumption costs you the whole problem.

**What to say out loud:**
> *"Let me make sure I've got this. I'm given an array of integers and a target, and I want to
> return the indices of the two numbers that sum to the target. Let me confirm a few things…"*

**Clarifying questions to keep in your back pocket** (ask the ones that actually matter):

| Category | Questions to ask |
|----------|------------------|
| Input shape | Sorted or unsorted? Can it be empty / null? Size range (the Big-O budget!)? |
| Values | Negatives? Duplicates? Zeros? Range of values (overflow risk)? |
| Output | Return value or in-place? Indices or values? Any specific order? |
| Edge cases | What if no answer exists? Multiple answers — any or all? Ties? |
| Guarantees | "Exactly one solution"? Can I assume valid input? |
| Resources | Constraints on time/space? Can I modify the input? |

> **The constraints are the strategy.** "n up to 10⁵" rules out `O(n²)` and points you at
> `O(n log n)` / `O(n)`. "n up to 20" *invites* exponential backtracking. Always read them — they
> are the interviewer telling you the intended complexity.

**Example.** Two Sum: *"Unsorted array, return the two indices. Confirm: exactly one solution
guaranteed? Can I use the same element twice? Negatives allowed? Can I assume non-null, length ≥ 2?"*

---

## M — Match a pattern

**What to do:** Map the problem to one of the ~15 recurring patterns. This is the single highest-
leverage skill in the whole course — "a new problem becomes a variation you've seen." Recognize the
pattern and the approach is half-written.

**What to say out loud:**
> *"This looks like a hashing problem — I need to find a complement quickly, which a HashMap gives me
> in O(1). It's the classic Two Sum shape."*

### Recognition cheat-sheet: signal → pattern

Memorize the *signals*, not the solutions. When you hear yourself describe the problem with one of
these phrases, it points to a pattern (full drills in [`../02-patterns/`](../02-patterns/)):

| Signal in the problem | Likely pattern | Drill |
|-----------------------|----------------|-------|
| Sorted array, find a pair/triplet, or "two ends moving in" | **Two Pointers** | [2.1](../02-patterns/01-two-pointers/) |
| Longest/shortest **contiguous** subarray/substring under a constraint | **Sliding Window** | [2.2](../02-patterns/02-sliding-window/) |
| Cycle detection, find middle of a linked list | **Fast & Slow Pointers** | [2.3](../02-patterns/03-fast-slow-pointers/) |
| Many **range-sum** queries, "subarray sums to K" | **Prefix Sum** | [2.4](../02-patterns/04-prefix-sum/) |
| Sorted input, or "search/minimize/maximize a value" | **Binary Search (incl. on the answer)** | [2.5](../02-patterns/05-binary-search/) |
| Overlapping intervals, meeting rooms, merge ranges | **Intervals & Sorting** | [2.6](../02-patterns/06-intervals/) |
| Generate **all** combinations/permutations/subsets; n is tiny | **Backtracking** | [2.7](../02-patterns/07-backtracking/) |
| "Number of ways," "min/max over choices," optimal substructure (1D state) | **DP — 1D** | [2.8](../02-patterns/08-dp-1d/) |
| Grid paths, edit distance, 2D state table | **DP — 2D & grids** | [2.9](../02-patterns/09-dp-2d/) |
| Knapsack, string matching, interval DP | **DP — advanced** | [2.10](../02-patterns/10-dp-advanced/) |
| "Pick the locally-best choice each step" proves optimal | **Greedy** | [2.11](../02-patterns/11-greedy/) |
| "Top K," "Kth largest/smallest," "merge K sorted," running median | **Heap / Top-K** | [2.12](../02-patterns/12-heap-topk/) |
| Nodes & edges, connectivity, shortest path, ordering with deps | **Graph algorithms** | [2.13](../02-patterns/13-graph-algorithms/) |
| Single number, masks, "without extra space," XOR tricks | **Bit Manipulation** | [2.14](../02-patterns/14-bit-manipulation/) |
| Matrix rotation, GCD, primes, coordinate geometry | **Math & Geometry** | [2.15](../02-patterns/15-math-matrix/) |
| "Fast lookup / have I seen this / count occurrences" | **Hashing** | [1.2](../01-data-structures/02-hashing/) |

> **Heuristic:** "contiguous + constraint" → sliding window; "sorted + pair" → two pointers; "fast
> lookup / seen-before / counts" → hashing; "all possibilities, small n" → backtracking; "min/max
> with overlapping subproblems" → DP; "top/Kth/merge-K" → heap.

---

## P — Plan / Pseudocode

**What to do:** Sketch the algorithm in plain steps **before** writing Java. Walk it on the example.
This is where you catch a flawed approach cheaply — fixing pseudocode costs seconds; rewriting code
costs minutes you don't have.

**What to say out loud:**
> *"Here's my plan: one pass, keeping a HashMap of value→index. For each number, check if its
> complement (target − num) is already in the map; if so I've found my pair; otherwise store it and
> move on. Let me trace it on `[2,7,11,15]`, target 9…"*

**Always brute-force first, then optimize.** State the obvious solution and its complexity, *then*
improve. This guarantees you have *something* working, demonstrates range, and the brute force often
reveals the optimization.

> *"The brute force is two nested loops checking every pair — O(n²). I can do better: the inner loop
> is really asking 'have I seen target − num before?', which a HashMap answers in O(1), giving O(n)."*

```
plan (pseudocode):
  seen = empty map (value -> index)
  for i, num in nums:
      need = target - num
      if need in seen: return [seen[need], i]
      seen[num] = i
  return []   // per constraints, unreachable
```

> Confirm the plan with your interviewer before coding: *"Does that approach sound reasonable before
> I implement it?"* It's a free checkpoint that prevents 15 minutes coding the wrong thing.

---

## I — Implement

**What to do:** Translate the agreed pseudocode into clean Java. Use good names, apply the toolkit
from [0.2](02-java-for-interviews.md), and keep narrating so the interviewer follows your intent.

**What to say out loud:**
> *"I'll use a `HashMap<Integer,Integer>`. I'm using `containsKey` before `get` to avoid unboxing a
> null. Note I store the index *after* the check so I don't match an element with itself…"*

```java
public int[] twoSum(int[] nums, int target) {
    Map<Integer,Integer> seen = new HashMap<>();      // value -> index
    for (int i = 0; i < nums.length; i++) {
        int need = target - nums[i];
        if (seen.containsKey(need)) return new int[]{seen.get(need), i};
        seen.put(nums[i], i);
    }
    return new int[]{};                                // no pair found
}
```

**Implementation tips:**
- Write the **happy path** first; handle edges once the core is right.
- Don't go silent. Narrate non-obvious lines — silence reads as "lost."
- Lean on the toolkit: `getOrDefault`, `lo + (hi - lo)/2`, `StringBuilder`, `Integer.compare`.
- If you blank on syntax (a `Comparator`, a `computeIfAbsent`), say so and write a clear stub — a
  small syntax slip with a clear intent is forgivable; a wrong algorithm isn't.

---

## R — Review

**What to do:** Before declaring "done," **trace your own code** line by line on a concrete example,
then deliberately attack the edge cases. Finding your own bug is a *strong* positive signal;
shipping a bug for the interviewer to find is the opposite.

**What to say out loud:**
> *"Let me trace `[3,2,4]`, target 6. i=0: need=3, not seen, store 3→0. i=1: need=4, not seen, store
> 2→1. i=2: need=2, seen at index 1 → return [1,2]. Correct. Now the edge cases…"*

**The edge-case checklist** — run through it every time:

| Category | Check |
|----------|-------|
| Empty / null | `[]`, `null`, empty string |
| Size 1 | single element / single node |
| Boundaries | first/last index, off-by-one on loop bounds |
| Duplicates | repeated values, all-same array |
| Extremes | negatives, zero, `Integer.MAX_VALUE` (overflow!) |
| No solution | does it return the right "not found" value? |
| Special structure | already sorted, all equal, strictly decreasing |

> **Test your own code unprompted.** Pick a small concrete input and *be the computer* — track every
> variable. This is where you catch off-by-ones, the unhandled empty input, the `==` on `Integer`s.

---

## E — Evaluate complexity

**What to do:** State **time and space**, justified in a phrase, and name any trade-off. (This is the
payoff of [0.1](01-big-o-complexity.md).) Then discuss whether you can do better.

**What to say out loud:**
> *"Time is O(n) — one pass over the array, with O(1) HashMap operations. Space is O(n) for the map
> in the worst case. I traded O(n) space to bring time down from the O(n²) brute force — a good deal
> here. If the array were sorted, I could use two pointers for O(1) space instead."*

Cover, every time: **time**, **space** (including recursion stack and output), the **worst case**,
and **"can we do better?"** — proposing the next optimization (even if you don't implement it) shows
depth.

---

## When you get stuck

Being stuck is expected — *how* you handle it is being tested. A productive ladder:

1. **Re-read the constraints.** The size hint often reveals the intended approach.
2. **Do a tiny example by hand.** Solving `n=3` manually frequently exposes the algorithm.
3. **Brute force, then optimize.** A working O(n²) beats a broken O(n). Get *something* down.
4. **Try a pattern from the cheat-sheet.** "Could two pointers / a hashmap / sorting help here?"
5. **Simplify.** Solve a restricted version (sorted input, no duplicates), then generalize.
6. **Think out loud and use your interviewer.** *"I'm weighing sorting first vs. a hashmap — sorting
   is O(n log n) but O(1) space…"* They will nudge you, and they're scoring your *reasoning*, not
   your silence.

> Never freeze silently. A narrated dead-end is far better than a quiet one — it lets the interviewer
> help and shows them how you think.

---

## Communication tips

The interview is a **collaboration**, not an exam. Across all six steps:
- **Think out loud, always.** Unspoken reasoning earns zero credit.
- **Signpost where you are.** *"Okay, I understand the problem; now let me find the pattern."*
- **Check in at decision points.** *"I'm going with a hashmap — sound good before I code?"*
- **Admit unknowns honestly.** *"I don't recall the exact `Comparator` syntax; I'll stub it and
  come back."* Honesty beats bluffing.
- **Be coachable.** A hint is help, not a failure — take it gracefully and integrate it.
- **State the pattern by name.** *"This is a sliding window because we want the longest contiguous
  substring under a constraint."* Interviewers hire pattern-recognition + clear reasoning.

---

## In the wild

UMPIRE isn't just an interview gimmick — it's how strong engineers approach unfamiliar problems
generally: understand requirements, recognize a known shape, sketch before building, implement,
test, and reason about cost. The discipline of "clarify, then plan, then build, then verify"
transfers directly to design docs and real features. The interview is a compressed, observable
version of the job.

---

## Interview angle

Two candidates can reach the same solution and get opposite verdicts: the one who clarified, named
the pattern, planned out loud, traced their own code, and stated complexity **passes**; the one who
silently typed a correct-but-unexplained answer often doesn't. The process *is* the signal. Run all
six steps even when the answer feels obvious — the muscle memory carries you through the problems
where it isn't.

**Common follow-ups:**
- "Can you do better?" → have the next optimization ready (sort? hash? two pointers?).
- "What if the input doesn't fit in memory?" → streaming / one-pass / external thinking.
- "Walk me through your edge cases." → your Review checklist, out loud.

---

## Self-check

1. What do the six letters of UMPIRE stand for, and what's the one-line goal of each step?
2. You're told `n ≤ 20` and asked to "find all valid groupings." What does the constraint suggest
   about the intended pattern and complexity?
3. Why state and analyze the brute force *before* optimizing — what does it buy you?
4. Name three signals from the cheat-sheet and the pattern each points to.
5. You're stuck with 10 minutes left. List the first three moves from the "when you get stuck"
   ladder.

---

You now have the language (Big-O), the toolkit (Java), and the process (UMPIRE). Time to make data
structures *real* — starting where half of all interview problems live.

**Next:** [1.1 — Arrays & Strings »](../01-data-structures/01-arrays-and-strings/)
