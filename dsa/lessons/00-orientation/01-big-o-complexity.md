# 0.1 — Big-O & Complexity Analysis

> **Part 0 · Orientation** · ~22 min read
> *Big-O is not a stopwatch. It's a way to predict how your code behaves as the input grows —
> the single most useful lens for "will this be fast enough?" in an interview.*

---

## The problem

You write a function. It runs in 3 milliseconds on your laptop with the test input. Is it "fast"?
You have no idea. Run it on a slower machine, or with 10× the data, and the answer changes. A
**stopwatch measures one run on one machine**; it tells you nothing about what happens when the
input doubles.

What interviewers actually care about is the **shape of the growth**: as the input gets big, does
the work grow proportionally? Quadratically? Barely at all? **Big-O** is the language for that
shape. It throws away the noise (your CPU, the constant overhead, the small inputs) and keeps the
one thing that matters at scale: how the work scales with `n`.

> **Analogy.** Big-O is the **slope of a road, not your current speed**. Two cars on a flat road
> vs. a steep hill: the flat-road car might be slower *right now*, but as the trip gets longer the
> hill-climber falls hopelessly behind. Big-O describes the hill. The "constant factor" (your
> engine, today's traffic) is your speed at this instant — real, but not what decides a long race.

---

## What Big-O actually means

Big-O describes an **upper bound on growth** as `n → ∞`. When we say an algorithm is `O(n)`, we
mean: *beyond some input size, the number of operations grows at most linearly with `n`.* Two rules
fall straight out of that definition, and they're the source of every "wait, why?" in this topic:

1. **Drop the constants.** `O(2n)`, `O(500n)`, `O(n/2)` are all just `O(n)`. Doubling the input
   doubles the work in every case — the slope is linear regardless of the multiplier. The constant
   is a real-world detail (and matters in practice!), but it's not the *shape*.
2. **Drop the lower-order terms.** `O(n² + n)` is `O(n²)`. For large `n`, `n²` dwarfs `n` so
   completely that `n` becomes rounding error. Keep only the term that dominates.

```
n = 1,000,000
  n²        = 1,000,000,000,000   ← totally dominates
  + n       =         1,000,000   ← negligible next to n²
  + 500     =               500   ← invisible
→ O(n²)
```

> This is *why* "drop constants and lower terms" isn't laziness — it's the literal definition.
> Big-O answers "how does it scale," and only the dominant term affects scaling.

---

## The common complexity classes

These seven cover ~95% of what you'll meet in interviews. Memorize the ordering; it's a ladder you
constantly try to climb *down*.

| Class        | Name          | "When `n` doubles, work…"      | Typical source |
|--------------|---------------|--------------------------------|----------------|
| `O(1)`       | constant      | …doesn't change                | array index, hash lookup, math |
| `O(log n)`   | logarithmic   | …grows by a constant amount    | binary search, balanced-tree op |
| `O(n)`       | linear        | …doubles                       | one pass over the input |
| `O(n log n)` | linearithmic  | …a bit more than doubles       | good sorts, divide-and-conquer |
| `O(n²)`      | quadratic     | …quadruples                    | nested loop over the input |
| `O(2ⁿ)`      | exponential   | …squares (explodes)            | subsets, naive recursion |
| `O(n!)`      | factorial     | …unspeakable                   | permutations, brute-force TSP |

### A table that makes it visceral: `n` vs. operations

This is the table to picture during an interview. It's the difference between "ships" and
"times out."

| n        | O(1) | O(log n) | O(n)      | O(n log n)   | O(n²)             | O(2ⁿ)                  |
|----------|------|----------|-----------|--------------|-------------------|------------------------|
| 10       | 1    | ~3       | 10        | ~33          | 100               | 1,024                  |
| 100      | 1    | ~7       | 100       | ~664         | 10,000            | ~1.3 × 10³⁰            |
| 1,000    | 1    | ~10      | 1,000     | ~9,966       | 1,000,000         | astronomically huge    |
| 1,000,000| 1    | ~20      | 1,000,000 | ~20,000,000  | 10¹² (≈ 16 min*)  | beyond the universe    |

\* Rough rule of thumb: a modern CPU does ~10⁸–10⁹ simple operations per second. So `10¹²`
operations ≈ minutes; `2¹⁰⁰` would outlast the heat death of the universe.

> **The interview heuristic.** If `n ≤ ~10⁶`, an `O(n)` or `O(n log n)` solution is comfortable.
> If `n ≤ ~5000`, `O(n²)` is usually fine. If `n ≤ ~20`, an exponential `O(2ⁿ)` brute force may be
> *expected* (subset/backtracking problems). The constraint size is a giant hint about the target
> complexity — read it.

### Each class, with a real example

**`O(1)` — constant.** Work independent of `n`. Indexing an array, a HashMap `get`, arithmetic.
```java
int x = a[i];                 // address arithmetic — same cost for any array size
boolean seen = set.contains(k); // hash lookup — O(1) average
```

**`O(log n)` — logarithmic.** Each step throws away a *fraction* (usually half) of the remaining
work. You can halve `1,000,000` only ~20 times before you hit 1 — that's why log is so flat.
```java
// binary search: search space halves every iteration
while (lo <= hi) { int mid = lo + (hi - lo) / 2; ... }
```

**`O(n)` — linear.** One pass. Sum an array, find a max, a single sliding window.
```java
for (int v : a) total += v;   // touch each element once
```

**`O(n log n)` — linearithmic.** The "good sort" tier. `Arrays.sort` on objects (merge sort),
divide-and-conquer that splits in half and does linear work per level.
```java
Arrays.sort(arr);             // n log n — and often the first thing you reach for
```

**`O(n²)` — quadratic.** A nested loop where both bounds scale with `n`. The most common
*accidental* complexity (e.g., string `+=` in a loop, or "compare every pair").
```java
for (int i = 0; i < n; i++)
  for (int j = 0; j < n; j++)   // n × n = n² pairs
    if (a[i] + a[j] == target) ...
```

**`O(2ⁿ)` — exponential.** Two choices at each of `n` items → `2ⁿ` combinations. Generating all
subsets; naive recursive Fibonacci (recomputes the same calls).
```java
// naive fib: T(n) = T(n-1) + T(n-2) ≈ 2^n calls — exponential
int fib(int n){ return n < 2 ? n : fib(n-1) + fib(n-2); }
```

**`O(n!)` — factorial.** All orderings of `n` items. Permutations, brute-force traveling salesman.
`10!` is 3.6 million; `13!` is over 6 billion. Only viable for tiny `n`.

---

## Time vs. space complexity

Big-O describes *any* resource that grows with input — most often **time** (operations) but also
**space** (extra memory). They're tracked **separately**, and interviewers want **both**.

- **Time:** how many operations as a function of `n`.
- **Space:** how much *extra* memory you allocate as a function of `n`. By convention this is
  **auxiliary** space — the input itself doesn't count, only what *you* add.

```
                input (not counted)        extra you allocate (counted)
two-pointer:    int[] a                     two index variables    → O(1) space
hashing pass:   int[] a                     a HashMap of n entries → O(n) space
```

There's almost always a **time–space trade-off**. Two Sum brute force is `O(n²)` time / `O(1)`
space; the HashMap version is `O(n)` time / `O(n)` space — you *spent memory to buy speed*. Naming
that trade-off out loud is exactly the reasoning interviewers reward.

> **Recursion costs space too.** Every recursive call adds a frame to the call stack. A recursion
> `n` levels deep is `O(n)` space *even if it allocates nothing* — the stack itself is the cost.
> (And in Java, too deep → `StackOverflowError`; more on that in 0.2.)

---

## Amortized analysis: the dynamic-array doubling story

Sometimes a single operation is occasionally expensive but *averages out* to cheap. That average,
spread over a sequence of operations, is the **amortized** cost — and it's a real, rigorous bound,
not hand-waving.

The canonical case: appending to a **dynamic array** (Java's `ArrayList`). It has a fixed-capacity
backing array. Most appends just drop a value in the next slot — `O(1)`. But when it's **full**, it
allocates a new array (usually **2× capacity**) and **copies everything over** — `O(n)`.

```
cap=4, full        append → grow to cap=8, copy 4, then add
[a b c d]                   [a b c d e _ _ _]
              copy cost: 4
```

Isn't that `O(n)` per append in the worst case? Per *operation* yes — but look at the total over
`n` appends. The expensive copies happen at sizes 1, 2, 4, 8, … and copy `1 + 2 + 4 + … + n`
elements total. That geometric series sums to **less than `2n`** — i.e., `O(n)` total work for `n`
appends. Divide by `n`: **`O(1)` amortized per append.**

```
copies over n appends:  1 + 2 + 4 + 8 + ... + n  ≈ 2n
total append work:      n (the inserts) + 2n (the copies) = 3n = O(n)
per append:             O(n)/n = O(1) amortized
```

> **Why 2× and not "+1 each time"?** Growing by a constant (e.g., +1) would make the copies sum to
> `1+2+3+…+n ≈ n²/2` → `O(n)` *per* append, `O(n²)` total. **Geometric** growth is what makes
> amortized `O(1)` work. You'll build exactly this in the Arrays & Strings assignment.

Say it in an interview as: *"Worst-case single append is O(n) during a resize, but amortized O(1)
because the doublings give a geometric copy series that sums to O(n) total."*

---

## How to analyze loops & recursion

### Loops — count the nesting against `n`

- **Sequential** loops **add**: `O(n) + O(n) = O(n)`.
- **Nested** loops **multiply**: a loop of `n` inside a loop of `n` is `O(n²)`.
- A loop whose counter **multiplies/divides** (e.g., `i *= 2`) runs `O(log n)` times, not `O(n)`.

```java
for (int i = 0; i < n; i++) { ... }          // O(n)
for (int i = 0; i < n; i++)
  for (int j = 0; j < n; j++) { ... }         // O(n²)
for (int i = 1; i < n; i *= 2) { ... }        // O(log n)  — i: 1,2,4,8,...
for (int i = 0; i < n; i++)
  for (int j = i+1; j < n; j++) { ... }       // O(n²) — still ~n²/2 pairs → drop the ½
```

> **Gotcha:** a hidden cost inside a loop multiplies in too. `for (...) s += someString;` looks
> `O(n)` but each `+=` rebuilds the string → secretly `O(n²)`. Always ask "what's the cost of the
> body?"

### Recursion — write the recurrence, then solve it

For recursive code, count: how many subproblems, how big each, and how much work *outside* the
recursive calls. That gives a **recurrence relation** `T(n)`.

```
binary search:   T(n) = T(n/2) + O(1)     → one half-size subproblem, constant work  → O(log n)
merge sort:      T(n) = 2·T(n/2) + O(n)   → two half-size subproblems, linear merge   → O(n log n)
naive fib:       T(n) = T(n-1) + T(n-2)   → two near-full subproblems                 → O(2ⁿ)
```

**Master-theorem-lite** — a fast intuition for the common `T(n) = a·T(n/b) + O(n^d)` shape (split
into `a` pieces of size `n/b`, with `O(n^d)` work to combine):

| Compare        | Result        | Why                                   | Example |
|----------------|---------------|---------------------------------------|---------|
| `a < bᵈ`       | `O(n^d)`      | top-level work dominates              | T(n)=2T(n/2)+n² → O(n²) |
| `a = bᵈ`       | `O(n^d log n)`| every level does equal work (log levels) | merge sort: 2T(n/2)+n → O(n log n) |
| `a > bᵈ`       | `O(n^{log_b a})` | the leaves (number of calls) dominate | T(n)=2T(n/2)+1 → O(n) |

You rarely need the formal theorem in an interview — but recognizing "merge sort = `2T(n/2)+n` =
`O(n log n)`" and "binary search = `T(n/2)+1` = `O(log n)`" on sight is expected.

---

## Best, average, worst case

The same algorithm can have different complexity depending on the *input*, not just its size.

| Case    | Means                          | Example: linear search for `x` in `n` items |
|---------|--------------------------------|---------------------------------------------|
| Best    | luckiest input                 | `x` is first → `O(1)` |
| Average | typical/random input           | `x` somewhere in the middle → `O(n)` |
| Worst   | adversarial input              | `x` last or absent → `O(n)` |

Unless told otherwise, **state the worst case** — it's the guarantee that matters for "will it pass
all test cases." Quicksort is the classic cautionary tale: `O(n log n)` average but `O(n²)` worst
(a bad pivot on already-sorted input). HashMap operations are `O(1)` *average* but `O(n)` worst
(everything hashes to one bucket). Mention the average when it's the realistic story, but flag the
worst when it bites.

---

## How to state complexity in an interview

A crisp, complete statement sounds like this:

> *"This is **O(n) time** — one pass over the array — and **O(n) space** for the HashMap. We could
> trade memory for nothing here, so I'd keep it. Worst case is the same since hashing is O(1)
> amortized."*

A checklist for what to always include:
- **Both** time and space. They'll ask for the missing one anyway.
- **Define `n`** (and `m`, `k`…) when there's more than one input dimension. "n = array length, k =
  number of distinct values" prevents ambiguity in answers like `O(n log k)`.
- **Justify in one phrase** — "one pass," "nested over all pairs," "halving each step."
- **Name the trade-off** if you made one ("I spent O(n) space to drop from O(n²) to O(n) time").
- **State the worst case**, and note the average if it's meaningfully better.

> **Multiple variables matter.** "Iterate over `m` strings of length `k`" is `O(m·k)`, *not*
> `O(n²)`. Collapsing distinct dimensions into one `n` is a common mistake — keep them separate.

---

## In the wild

Big-O is a model, and the model leaks. Real performance also depends on the **constant factor**
(an `O(n)` with huge per-step work can lose to an `O(n log n)` with tiny steps for realistic `n`),
on **cache locality** (a contiguous `int[]` scan crushes a pointer-chasing linked list even though
both are `O(n)`), and on the actual input distribution. Production engineers profile *and* reason
about Big-O — the asymptotics tell you which approach survives growth; the profiler tells you the
constants. In interviews you'll be judged almost entirely on the asymptotics, but knowing the model
has limits is itself a senior signal.

---

## Interview angle

Complexity analysis isn't a footnote — it's half the conversation. The pattern that scores:
1. State the brute-force complexity first ("the obvious approach is O(n²)…").
2. Use the **constraints** to set a target ("…but n is up to 10⁵, so O(n²) is ~10¹⁰ — too slow; I
   need O(n) or O(n log n)").
3. Optimize toward that target, narrating the trade-off.

**Common follow-ups:**
- "Can you do better than O(n²)?" → usually "sort it (n log n)" or "hash it (n time, n space)."
- "What's the space complexity?" → don't forget recursion stack and output size.
- "What's the worst case?" → especially for quicksort, hashing, and skewed trees.

---

## Self-check

1. Why is `O(n² + 100n + 5000)` simplified to `O(n²)`? Tie your answer to the *definition* of Big-O.
2. An algorithm does `10⁹` operations. Roughly how long is that on a modern CPU, and what does that
   imply for the largest `n` an `O(n²)` solution can handle in ~1 second?
3. Explain why appending to a dynamic array is `O(1)` amortized but `O(n)` worst case. Why does
   doubling (not adding a constant) make this work?
4. Give the recurrence for merge sort and for binary search, and the complexity each solves to.
5. `Integer` HashMap `get` is "O(1)." Under what input does it degrade, and to what?

---

**Next:** [0.2 — Java for interviews: the essential toolkit + gotchas »](02-java-for-interviews.md)
