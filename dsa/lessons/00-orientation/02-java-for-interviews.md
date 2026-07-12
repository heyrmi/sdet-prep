# 0.2 — Java for Interviews: The Essential Toolkit + Gotchas

> **Part 0 · Orientation** · ~30 min read
> *Java is a superb interview language — once you know its sharp edges. This is the toolkit you'll
> reach for in 90% of problems, plus the gotchas that quietly turn a correct idea into a wrong
> answer or an accidental O(n²).*

---

## The problem

Most interview algorithms are language-agnostic ideas. But you express them in Java, and Java has a
handful of edges that cost candidates real points: an `Integer ==` that lies, a `PriorityQueue`
comparator that overflows, a string concat that's secretly quadratic, a `.length` vs `.length()`
vs `.size()` slip. None are hard once you've seen them — but you have to have *seen* them, under
pressure, before. This lesson is that pre-exposure.

> **Analogy.** Knowing the algorithm is knowing how to drive. This lesson is learning **where this
> particular car's blind spots are**. You won't think about them once they're muscle memory — but
> hit one unaware and you crash in front of the interviewer.

---

## Arrays

The primitive workhorse. Fixed size, contiguous, fast.

```java
int[] a = new int[5];          // {0,0,0,0,0}  — primitives default to 0
int[] b = {3, 1, 4, 1, 5};     // literal init
int[][] grid = new int[3][4];  // 3 rows, 4 cols, all 0
int n = a.length;              // FIELD, no parens
```

Essential `Arrays` utilities:
```java
Arrays.sort(a);                       // in place, ascending; dual-pivot quicksort for primitives
Arrays.fill(a, -1);                   // set every slot to -1
int[] c = Arrays.copyOf(a, a.length); // real copy (b = a only aliases!)
int[] d = Arrays.copyOfRange(a, 1, 4);// indices [1,4)
boolean same = Arrays.equals(a, b);   // CONTENT equality (a == b is reference)
String s = Arrays.toString(a);        // "[3, 1, 4]" — debugging lifesaver
```

> **`int[]` is not `Integer[]`.** Primitive arrays don't play with generics/streams the same way.
> `Arrays.asList(intArray)` gives a `List<int[]>` of size **1** — a list containing the array — not
> a list of ints. For a stream of ints use `Arrays.stream(intArray)` (an `IntStream`). When you
> need `List<Integer>` or `Collections.sort` with a custom comparator on the values, you need
> `Integer[]` (boxed). Default of `new Integer[3]` is `{null, null, null}`, not zeros.

---

## ArrayList — the dynamic array

Your default resizable list. Amortized `O(1)` append (the doubling story from 0.1).

```java
List<Integer> list = new ArrayList<>();
list.add(5);              // append, amortized O(1)
list.get(0);             // O(1) index access
list.set(0, 9);          // O(1) overwrite
list.size();             // SIZE — method, with parens (not .length!)
list.remove(0);          // O(n) — by INDEX here (shifts the rest)
list.contains(5);        // O(n) — linear scan
int[] arr = list.stream().mapToInt(Integer::intValue).toArray(); // to int[]
```

> **`remove` overload trap.** On `List<Integer>`, `list.remove(2)` removes the **element at index
> 2** (`int` arg → by index), but `list.remove(Integer.valueOf(2))` removes the **value 2** (Object
> arg → by value). Pick deliberately — this surprises people constantly.

---

## HashMap & HashSet — your most-used tool

If you remember one data structure for interviews, it's the hash map: average `O(1)` insert, lookup,
delete. Half of "can you do better than O(n²)?" answers are "use a HashMap."

```java
Map<String,Integer> map = new HashMap<>();
Set<Integer> seen = new HashSet<>();

map.put("a", 1);
map.get("a");                 // 1; returns null if absent (NPE risk if you unbox!)
map.containsKey("a");
seen.add(5); seen.contains(5);
```

The convenience methods that write **clean, idiomatic** Java — interviewers notice these:

```java
// count frequencies — three equivalent idioms, cleanest last
map.put(c, map.getOrDefault(c, 0) + 1);     // explicit default
map.merge(c, 1, Integer::sum);             // "add 1, or start at 1" — my favorite for counting
map.computeIfAbsent(k, x -> new ArrayList<>()).add(v);  // group-into-buckets without null checks

map.putIfAbsent(k, v);        // set only if not present
```

Iterating — use `entrySet()` to get key and value in one pass (don't call `get` inside a `keySet`
loop, that's a redundant lookup):
```java
for (Map.Entry<String,Integer> e : map.entrySet()) {
    String k = e.getKey();
    int v = e.getValue();
}
```

> **`get` returns `null` for missing keys.** Assigning to an `int` auto-unboxes → `NullPointerException`.
> Use `getOrDefault` or `containsKey` first. And a `HashMap` has **no order** — never assume
> iteration order (use `LinkedHashMap` for insertion order, `TreeMap` for sorted).

---

## TreeMap & TreeSet — when you need order

Backed by a balanced BST (red-black tree). Operations are `O(log n)` instead of `O(1)`, but keys
stay **sorted**, and you get range/neighbor queries a HashMap can't do.

```java
TreeMap<Integer,String> tm = new TreeMap<>();
TreeSet<Integer> ts = new TreeSet<>();

ts.first();          // smallest
ts.last();           // largest
ts.floor(x);         // largest element ≤ x   (null if none)
ts.ceiling(x);       // smallest element ≥ x  (null if none)
ts.lower(x);         // largest element STRICTLY < x
ts.higher(x);        // smallest element STRICTLY > x
ts.headSet(x); ts.tailSet(x);  // sub-views
```

> **`floor`/`ceiling` return `null`** when nothing qualifies — guard before unboxing. Reach for
> `TreeMap`/`TreeSet` on signals like "next greater," "closest value," "range of keys," or "keep a
> sorted set as you go."

---

## ArrayDeque — stack AND queue in one class

`ArrayDeque` is the modern choice for **both** stacks and queues. It's faster than the legacy
`Stack` (synchronized, old) and `LinkedList` (pointer-chasing, cache-unfriendly).

```java
Deque<Integer> dq = new ArrayDeque<>();

// AS A STACK (LIFO): push/pop/peek operate on the HEAD
dq.push(1); dq.push(2);     // [2, 1]
dq.pop();                   // 2  (removes head)
dq.peek();                  // 1  (head, no removal)

// AS A QUEUE (FIFO): offer at tail, poll/peek at head
dq.offer(1); dq.offer(2);   // [1, 2]
dq.poll();                  // 1  (removes head)
dq.peek();                  // 2  (head)
```

| Want | Use these | Removes/reads from |
|------|-----------|--------------------|
| Stack (LIFO) | `push`, `pop`, `peek` | head |
| Queue (FIFO) | `offer`, `poll`, `peek` | head (read), tail (`offer` adds) |

> **Never store `null` in an `ArrayDeque`.** It uses `null` as the "empty" sentinel — so `poll`/
> `peek` return `null` to mean "empty," and pushing a real `null` corrupts that. Also prefer the
> `offer`/`poll`/`peek` family over `add`/`remove`/`element`: the former **return null/false** on
> empty, the latter **throw**. Returning is usually what you want in a loop.

---

## PriorityQueue — the heap

A binary heap. `offer`/`poll` are `O(log n)`; `peek` is `O(1)`. The go-to for "top-K," "Kth
largest," "merge K sorted," and "always grab the smallest/largest next."

```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();           // MIN-heap by default!
minHeap.offer(5); minHeap.offer(1); minHeap.offer(3);
minHeap.poll();   // 1  — smallest comes out first
minHeap.peek();   // 3  — next smallest, no removal
```

> **It's a min-heap by default** — `poll` returns the *smallest*. People assume max-heap and get it
> backwards. For a **max-heap**, reverse the comparator:

```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
// or:                            new PriorityQueue<>((x, y) -> y - x);   // ← BUGGY, see below
```

**The overflow comparator bug — drill this one.** The "clever" `(a, b) -> a - b` comparator
**overflows** for large/negative ints: `a - b` can wrap past `Integer.MAX_VALUE` and flip sign,
silently corrupting order.

```java
// BUG: a - b overflows for extreme values (e.g. a=2e9, b=-2e9)
new PriorityQueue<>((a, b) -> a - b);

// SAFE: Integer.compare never overflows
new PriorityQueue<>((a, b) -> Integer.compare(a, b));
new PriorityQueue<>(Comparator.reverseOrder());          // for max-heap
```

Heaps of objects with a custom key (e.g., heap of `int[]{value, index}` by value):
```java
PriorityQueue<int[]> pq = new PriorityQueue<>((x, y) -> Integer.compare(x[0], y[0]));
```

---

## StringBuilder — and the O(n²) concat trap

Strings are **immutable**. Every `s + x` allocates a brand-new string and copies the old contents.
In a loop, that's `O(n²)` — a classic silent bug.

```java
// BAD: O(n²) — rebuilds the whole string every iteration
String r = "";
for (char c : chars) r += c;

// GOOD: O(n) — one growable buffer
StringBuilder sb = new StringBuilder();
for (char c : chars) sb.append(c);
String r = sb.toString();
```

Useful `StringBuilder` ops: `append`, `sb.reverse()`, `sb.charAt(i)`, `sb.setCharAt(i, c)`,
`sb.deleteCharAt(i)`, `sb.length()`.

---

## char arithmetic — the frequency-array superpower

A `char` **is** a 16-bit integer. `'a'` is 97. So `c - 'a'` maps `'a'..'z'` to `0..25` — perfect
for a fixed `int[26]` counter, which beats a HashMap for lowercase-letter problems (less overhead,
no boxing).

```java
int[] freq = new int[26];
for (char c : s.toCharArray()) freq[c - 'a']++;   // index by letter

char d = (char) ('a' + 3);     // 'd' — cast back to char after arithmetic
boolean isDigit = c >= '0' && c <= '9';
int digit = c - '0';           // '7' → 7
```

> Use `int[128]` for ASCII or `int[256]` for extended. `s.charAt(i)` returns a `char`;
> `s.toCharArray()` gives a mutable `char[]`; `String.valueOf(charArray)` converts back.

---

## int overflow — the binary-search & sum landmine

`int` is 32-bit, range about ±2.1 billion. Arithmetic that *temporarily* exceeds it **wraps
silently** (no exception). Two places this bites constantly:

```java
// Binary search midpoint: lo + hi can overflow when both are large
int mid = (lo + hi) / 2;        // BUG: lo + hi may overflow
int mid = lo + (hi - lo) / 2;   // SAFE: never exceeds hi

// Summing or multiplying large values: promote to long
long sum = 0;
for (int v : a) sum += v;       // use long if the total can exceed ~2.1e9
long prod = (long) x * y;       // cast BEFORE multiplying; (long)(x*y) is too late
```

> `2147483647 + 1` is `-2147483648`. Whenever a sum/product/midpoint *might* exceed ~2.1e9, reach
> for `long`, and cast **before** the operation that overflows, not after.

---

## Autoboxing & the `Integer ==` cache trap

Java auto-converts between `int` (primitive) and `Integer` (object). The trap: `==` on two
`Integer` objects compares **references**, not values.

```java
Integer a = 100, b = 100;
System.out.println(a == b);   // true  — cached!
Integer x = 200, y = 200;
System.out.println(x == y);   // FALSE — different objects!
System.out.println(x.equals(y)); // true — value comparison
```

Why the inconsistency? Java **caches** `Integer` objects for `-128..127`, so small values reuse the
same object and `==` *accidentally* works. Above 127 you get fresh objects and `==` breaks. This is
the cruelest bug because it passes your small test cases and fails the big ones.

> **Rules:** compare `Integer`s with `.equals()` or unbox to `int` first (`a.intValue() == b`).
> Prefer `int` over `Integer` for loop counters and keys you do arithmetic on. Autoboxing also has
> a hidden cost — boxing millions of ints in an `ArrayList<Integer>` is slower and heavier than an
> `int[]`.

---

## Comparator / Comparable + lambdas

`Comparable` = a type's **natural order** (`compareTo`). `Comparator` = an **external** order you
supply. The contract: return negative if `a` before `b`, 0 if equal, positive if `a` after `b`.

```java
// sort objects by a field, ascending
Arrays.sort(people, (p, q) -> Integer.compare(p.age, q.age));
Arrays.sort(people, Comparator.comparingInt(p -> p.age));        // cleaner
people.sort(Comparator.comparingInt((Person p) -> p.age)
                       .thenComparing(p -> p.name));             // tie-break
people.sort(Comparator.comparingInt((Person p) -> p.age).reversed()); // descending

Collections.sort(list);                       // natural order (Comparable)
Collections.sort(list, Comparator.reverseOrder());
```

> Same overflow rule as the heap: use `Integer.compare(x, y)`, never `x - y`, inside comparators.

---

## 2D arrays

```java
int[][] grid = new int[rows][cols];   // all zeros
int[][] g = {{1,2,3},{4,5,6}};
int r = g.length;       // number of rows
int c = g[0].length;    // number of cols (assumes rectangular)
for (int[] row : g) for (int v : row) { ... }

// 4-directional neighbor traversal (BFS/DFS on grids) — memorize this idiom
int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};
for (int[] d : DIRS) {
    int nr = row + d[0], nc = col + d[1];
    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) { ... }  // bounds check FIRST
}
```

> Java 2D arrays are **arrays of arrays** (can be jagged). Always bounds-check before indexing —
> grid problems live and die on the `0 <= nr < rows && 0 <= nc < cols` guard.

---

## Math helpers

```java
Math.max(a, b); Math.min(a, b); Math.abs(x);
Math.pow(2, 10);            // returns DOUBLE (1024.0) — cast if you need int
(int) Math.sqrt(n);        // floor of sqrt
Math.floorMod(-1, 3);      // 2  — true modulo (% gives -1 for negatives!)
Integer.MAX_VALUE;         // 2147483647  — common "infinity" sentinel
Long.MAX_VALUE;            // bigger sentinel when ints can overflow
```

> **`%` is remainder, not modulo:** `-1 % 3` is `-1` in Java, not `2`. For wrap-around (circular
> arrays, hashing), use `Math.floorMod` or `((x % m) + m) % m`.

---

## Recursion & stack depth

Each recursive call uses a stack frame. Java's default thread stack holds only **~10k–20k frames**;
deeper recursion throws `StackOverflowError` (not an exception you'd normally catch).

```java
// depth-n recursion = O(n) stack space, and risky for large n
void dfs(Node x) { if (x == null) return; dfs(x.left); dfs(x.right); }
```

> A linked list / skewed tree of length 10⁵ will **blow the stack** if you recurse naively —
> convert to an **iterative** approach with an explicit `ArrayDeque` stack when depth can be large.
> Always count the recursion stack in your space complexity.

---

## Gotchas at a glance

| # | Gotcha | Wrong | Right |
|---|--------|-------|-------|
| 1 | length spelling | `arr.length()` / `list.length` | `arr.length`, `str.length()`, `list.size()` |
| 2 | `Integer ==` cache | `a == b` for `Integer` | `a.equals(b)` or unbox to `int` |
| 3 | binary-search overflow | `(lo + hi) / 2` | `lo + (hi - lo) / 2` |
| 4 | sum/product overflow | `int sum`/`x * y` | `long sum`, `(long) x * y` |
| 5 | comparator overflow | `(a, b) -> a - b` | `(a, b) -> Integer.compare(a, b)` |
| 6 | PriorityQueue default | assuming max-heap | it's a **min**-heap; reverse for max |
| 7 | string concat in loop | `s += c` | `StringBuilder.append` |
| 8 | `map.get` on missing key | unbox a `null` → NPE | `getOrDefault` / `containsKey` |
| 9 | array compare/copy | `a == b`, `b = a` | `Arrays.equals`, `Arrays.copyOf` |
| 10 | `List.remove(int)` | by-index vs by-value mixup | `remove(Integer.valueOf(x))` for value |
| 11 | `null` in `ArrayDeque` | `dq.push(null)` | never store null (it's the sentinel) |
| 12 | negative `%` | `-1 % 3 == -1` | `Math.floorMod(-1, 3) == 2` |
| 13 | `Arrays.asList(int[])` | `List<int[]>` of size 1 | use `Arrays.stream(arr)` |
| 14 | `Math.pow` returns double | `int p = Math.pow(...)` | cast, or loop-multiply for exact ints |
| 15 | deep recursion | recurse on 10⁵-deep input | iterate with explicit stack |

---

## In the wild

These idioms aren't interview-only — `computeIfAbsent` for building adjacency lists, `ArrayDeque`
over `Stack`, `Integer.compare` in comparators, and `StringBuilder` for any non-trivial string
assembly are exactly what you'd write in production Java. The `Integer` cache and `int` overflow
bugs are real CVEs and outage post-mortems, not academic curiosities — binary-search overflow
famously lurked in the JDK's own `Arrays.binarySearch` for nearly a decade.

---

## Interview angle

You won't be graded on Java trivia directly, but the gotchas are **silent point-losers**: a
solution that's algorithmically perfect but overflows, or compares `Integer`s with `==`, reads as
"hasn't shipped much Java." Conversely, reaching for `getOrDefault`, an `int[26]` frequency array,
or `lo + (hi - lo) / 2` *unprompted* signals fluency. Pick `ArrayDeque` over `Stack`, state
"min-heap by default" when you instantiate a `PriorityQueue`, and narrate "I'll use `long` here to
avoid overflow."

**Common follow-ups:**
- "Why `StringBuilder`?" → string immutability makes `+=` O(n²).
- "Does this overflow?" → check sums, products, and midpoints; reach for `long`.
- "What's the space?" → include the HashMap *and* the recursion stack.

---

## Self-check

1. `Integer a = 128, b = 128; a == b` — true or false, and exactly why? What about `127`?
2. Why is `(lo + hi) / 2` a bug in binary search, and what's the fix? What's the analogous fix for
   summing a large array?
3. What's wrong with `new PriorityQueue<>((a, b) -> a - b)` and how do you fix it? Is a default
   `PriorityQueue` a min- or max-heap?
4. Give the three spellings for "how many elements" across `int[]`, `String`, and `List`.
5. What does `Arrays.asList(myIntArray)` return, and why is it surprising?
6. Why can deep recursion on a 10⁵-length linked list crash, and what's the workaround?

---

**Next:** [0.3 — A problem-solving framework (UMPIRE) »](03-problem-solving-framework.md)
