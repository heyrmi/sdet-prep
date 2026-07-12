# Glossary

Plain-English definitions of every term used across this course — skim it when a word in a lesson is unfamiliar, or read it end-to-end as a vocabulary warm-up. Links point to the lesson where each idea is taught.

---

## A

**Adjacency list** — A graph stored as, for each vertex, a list of its neighbors. Space `O(V + E)` and the default for most interview graphs (sparse, few edges); built cleanly in Java with `computeIfAbsent`. (see [1.7](01-data-structures/07-graphs/))

**Adjacency matrix** — A graph stored as a `V × V` grid where cell `[i][j]` marks whether an edge exists from `i` to `j`. `O(1)` edge lookup but `O(V²)` space — only worth it for dense graphs. (see [1.7](01-data-structures/07-graphs/))

**Amortized analysis** — Averaging the cost of an operation over a long run, so occasional expensive steps are spread out. A dynamic array's append is amortized `O(1)`: most appends are instant, the rare resize is `O(n)`, but it averages out to constant. (see [0.1](00-orientation/01-big-o-complexity.md))

**ArrayDeque** — Java's resizable double-ended queue, the modern choice for both stacks and queues (faster than `Stack` or `LinkedList`). Never store `null` in it — `null` is its "empty" sentinel. (see [0.2](00-orientation/02-java-for-interviews.md))

**ArrayList** — Java's dynamic (resizable) array. `O(1)` indexed access and amortized `O(1)` append, but `O(n)` to insert/remove in the middle. Note `.size()` (method), not `.length`. (see [0.2](00-orientation/02-java-for-interviews.md))

**Array** — A fixed-size, contiguous block of same-typed elements with `O(1)` access by index. The primitive workhorse; in Java `arr.length` is a field (no parens) and size can't change after creation. (see [1.1](01-data-structures/01-arrays-and-strings/))

**Arithmetic shift** — A bit shift that preserves the sign of a signed number by copying the sign bit into vacated positions. In Java, `>>` is arithmetic (sign-extending); contrast with `>>>` (logical). (see [2.14](02-patterns/14-bit-manipulation/))

**Autoboxing** — Java's automatic conversion between a primitive (`int`) and its object wrapper (`Integer`). Convenient but a trap: it can hide `NullPointerException`s when unboxing a `null`, and adds memory/CPU overhead in tight loops. (see [0.2](00-orientation/02-java-for-interviews.md))

**Average case** — The expected running time over typical/random inputs, as opposed to the worst possible input. A hash map lookup is average-case `O(1)` but worst-case `O(n)` if everything collides. (see [0.1](00-orientation/01-big-o-complexity.md))

---

## B

**Backtracking** — A brute-force-with-undo technique: build a candidate solution step by step, and the moment a partial choice can't lead anywhere valid, abandon it ("backtrack") and try the next. The engine behind permutations, subsets, and N-Queens. (see [2.7](02-patterns/07-backtracking/))

**Balanced tree** — A tree kept short and bushy so its height stays `O(log n)`, guaranteeing fast operations. Java's `TreeMap`/`TreeSet` use a self-balancing red-black tree under the hood. (see [1.5](01-data-structures/05-trees-and-bst/))

**Base case** — The condition in a recursive function that stops the recursion (e.g. "if the node is null, return"). Without a reachable base case, recursion never ends and you get a `StackOverflowError`. (see [1.5](01-data-structures/05-trees-and-bst/))

**Bellman-Ford** — A shortest-path algorithm that handles negative edge weights (which Dijkstra can't), running in `O(V·E)` by relaxing all edges `V−1` times. It can also detect negative-weight cycles. (see [2.13](02-patterns/13-graph-algorithms/))

**Best case** — The fastest an algorithm can run, given its luckiest input. Less interview-relevant than worst/average case, but useful context — e.g. insertion sort is `O(n)` best case on already-sorted data. (see [0.1](00-orientation/01-big-o-complexity.md))

**BFS (breadth-first search)** — A graph/tree traversal that explores level by level using a queue, visiting all closest nodes before farther ones. The go-to for shortest path in an unweighted graph. (see [1.7](01-data-structures/07-graphs/))

**Big-O** — Notation describing how an algorithm's cost grows as input size `n` grows, ignoring constants and lower-order terms (e.g. `O(n)`, `O(n log n)`, `O(n²)`). It answers "does this scale?", not "how many milliseconds?". (see [0.1](00-orientation/01-big-o-complexity.md))

**Binary heap** — A complete binary tree stored in an array where every parent is ≤ (min-heap) or ≥ (max-heap) its children. Gives `O(log n)` insert/remove and `O(1)` peek at the extreme. Java's `PriorityQueue` is one. (see [1.6](01-data-structures/06-heaps/))

**Binary search** — Repeatedly halving a *sorted* search range to find a target in `O(log n)`. In Java, compute the midpoint as `lo + (hi - lo) / 2` to dodge integer overflow. (see [2.5](02-patterns/05-binary-search/))

**Binary search on the answer** — Binary-searching over the *space of possible answers* rather than over array indices, used when "is answer `x` feasible?" gets easier as `x` grows (a monotonic predicate). Classic for "minimize the maximum" problems. (see [2.5](02-patterns/05-binary-search/))

**Binary search tree (BST)** — A binary tree where every left descendant is smaller and every right descendant is larger than a node. This ordering gives `O(log n)` search/insert/delete *when balanced* — but degrades to `O(n)` when skewed. (see [1.5](01-data-structures/05-trees-and-bst/))

**Binary tree** — A tree where each node has at most two children (left and right). The foundation for BSTs, heaps, and most tree-traversal problems. (see [1.5](01-data-structures/05-trees-and-bst/))

**Bit manipulation** — Operating directly on the binary bits of a number with operators like AND (`&`), OR (`|`), XOR (`^`), NOT (`~`), and shifts (`<<`, `>>`). Enables compact, fast tricks for sets, flags, and arithmetic. (see [2.14](02-patterns/14-bit-manipulation/))

**Bitmask** — Using the bits of a single integer as an array of on/off flags, so a subset of up to ~32 items fits in one `int`. Central to bitmask dynamic programming over small sets. (see [2.14](02-patterns/14-bit-manipulation/))

**BIT (binary indexed tree)** — See *Fenwick tree*. (see [1.10](01-data-structures/10-advanced-trees/))

---

## C

**Call stack** — The runtime's stack of in-progress function calls; each call adds a frame holding its local state, popped when the call returns. Recursion depth = stack frames used, which is why deep recursion costs `O(depth)` space. (see [0.2](00-orientation/02-java-for-interviews.md))

**Collision** — When two distinct keys hash to the same bucket in a hash table. Unavoidable in general, so hash tables need a resolution strategy (separate chaining or open addressing). (see [1.2](01-data-structures/02-hashing/))

**Comparable** — A Java interface a type implements to define its *natural ordering* via `compareTo` (e.g. numbers sort ascending). Used by `Collections.sort(list)` with no extra argument. (see [0.2](00-orientation/02-java-for-interviews.md))

**Comparator** — An *external* ordering you supply as a separate object/lambda, returning negative/zero/positive for "before/equal/after". Use `Integer.compare(a, b)`, never `a - b`, to avoid overflow. (see [0.2](00-orientation/02-java-for-interviews.md))

**Complete tree** — A binary tree where every level is full except possibly the last, which fills left to right. This shape is what lets a heap live compactly in an array. (see [1.6](01-data-structures/06-heaps/))

**Connected component** — A maximal group of vertices that can all reach each other in an undirected graph. Counting them is a classic BFS/DFS or union-find task (e.g. "number of islands"). (see [1.7](01-data-structures/07-graphs/))

**Constraint satisfaction** — A problem framed as "assign values to variables so that all rules hold" (e.g. Sudoku, N-Queens). Backtracking is the standard solver, pruning branches that violate a constraint early. (see [2.7](02-patterns/07-backtracking/))

**Cycle** — A path in a graph that returns to its start. Detecting cycles matters for deadlock detection, topological sorting (a DAG has none), and linked-list loop problems. (see [1.7](01-data-structures/07-graphs/))

---

## D

**DAG (directed acyclic graph)** — A directed graph with no cycles. Because it has a consistent ordering, a DAG can be topologically sorted — the basis for task scheduling and dependency resolution. (see [2.13](02-patterns/13-graph-algorithms/))

**Decision tree** — The conceptual tree of all choices a backtracking algorithm could make, where each path from root to leaf is one candidate solution. Visualizing it helps you spot where to prune. (see [2.7](02-patterns/07-backtracking/))

**Deque** — A double-ended queue: you can add and remove from both ends in `O(1)`. Java's `ArrayDeque` implements it and doubles as a stack or queue. (see [1.4](01-data-structures/04-stacks-and-queues/))

**DFS (depth-first search)** — A traversal that goes as deep as possible down one path before backtracking, using recursion or an explicit stack. The natural fit for tree traversals, connected components, and cycle detection. (see [1.7](01-data-structures/07-graphs/))

**Dijkstra** — A shortest-path algorithm for graphs with non-negative edge weights, using a min-heap to always expand the closest unsettled vertex. Runs in `O(E log V)`. Fails with negative edges. (see [2.13](02-patterns/13-graph-algorithms/))

**Directed graph** — A graph whose edges have a direction (one-way), like "A follows B". Contrast with undirected, where edges go both ways. (see [1.7](01-data-structures/07-graphs/))

**Doubly linked list** — A linked list where each node points to both its next and previous node, allowing `O(1)` removal given a node reference. Costs extra memory per node for the back-pointer. (see [1.3](01-data-structures/03-linked-lists/))

**DSU (disjoint-set union)** — See *Union-Find*. (see [1.9](01-data-structures/09-union-find/))

**Dummy node** — A throwaday placeholder node placed before a linked list's real head, so you never special-case "the list is empty" or "we're modifying the head." Also called a sentinel node. (see [1.3](01-data-structures/03-linked-lists/))

**Dynamic array** — An array that grows automatically by allocating a bigger backing array (typically doubling) and copying over when full. This doubling makes append amortized `O(1)`; Java's `ArrayList` is one. (see [1.1](01-data-structures/01-arrays-and-strings/))

**Dynamic programming (DP)** — Solving a problem by breaking it into overlapping subproblems and reusing their answers instead of recomputing. Requires optimal substructure; implemented via memoization (top-down) or tabulation (bottom-up). (see [2.8](02-patterns/08-dp-1d/))

---

## E

**Edge** — A connection between two vertices in a graph. May be directed or undirected, weighted or unweighted. (see [1.7](01-data-structures/07-graphs/))

**Edit distance** — The minimum number of single-character insertions, deletions, or substitutions to turn one string into another (a.k.a. Levenshtein distance). A canonical 2D DP, `O(m·n)`. (see [2.10](02-patterns/10-dp-advanced/))

**Euclid's algorithm** — A fast method for the greatest common divisor: repeatedly replace `(a, b)` with `(b, a mod b)` until `b` is 0. Runs in `O(log min(a, b))`. (see [2.15](02-patterns/15-math-matrix/))

**Exchange argument** — A proof technique for greedy algorithms: show that any optimal solution can be transformed into the greedy one by swapping choices without getting worse, proving greedy is optimal. (see [2.11](02-patterns/11-greedy/))

**equals/hashCode contract** — Java's rule that if two objects are `.equals()`, they must return the same `hashCode()`. Break it and hash maps/sets silently misbehave (lost keys, duplicates). Override both together, or neither. (see [1.2](01-data-structures/02-hashing/))

---

## F

**Fast & slow pointers** — A two-pointer technique where one pointer moves twice as fast as the other, used to find a list's middle or detect a cycle. The fast pointer "laps" the slow one inside any loop. (see [2.3](02-patterns/03-fast-slow-pointers/))

**Fast exponentiation** — Computing `base^exp` in `O(log exp)` by squaring repeatedly (`x^8 = ((x²)²)²`) instead of multiplying `exp` times. Also called binary exponentiation; extends to matrix power. (see [2.15](02-patterns/15-math-matrix/))

**Fenwick tree (BIT)** — A compact array structure giving `O(log n)` prefix-sum queries and point updates — lighter and simpler than a segment tree when you only need sums. (see [1.10](01-data-structures/10-advanced-trees/))

**FIFO (first-in, first-out)** — The ordering of a queue: the first element added is the first removed, like a line at a checkout. (see [1.4](01-data-structures/04-stacks-and-queues/))

**Fixed-size sliding window** — A sliding-window variant where the window length is constant; you slide it one step by adding the new element and dropping the oldest, keeping each step `O(1)`. (see [2.2](02-patterns/02-sliding-window/))

**Floyd's cycle detection** — The fast-and-slow-pointer algorithm (a.k.a. the "tortoise and hare") for detecting a cycle in a linked list and finding its start, using `O(1)` extra space. (see [2.3](02-patterns/03-fast-slow-pointers/))

---

## G

**GCD (greatest common divisor)** — The largest integer dividing two numbers evenly. Computed efficiently with Euclid's algorithm. (see [2.15](02-patterns/15-math-matrix/))

**Graph** — A set of vertices (nodes) connected by edges, modeling networks, maps, dependencies, and relationships. May be directed/undirected and weighted/unweighted. (see [1.7](01-data-structures/07-graphs/))

**Greedy** — An approach that makes the locally best choice at each step, hoping it yields a global optimum. Fast and simple, but only correct when the problem has the greedy-choice property. (see [2.11](02-patterns/11-greedy/))

**Greedy-choice property** — The condition that justifies a greedy algorithm: a globally optimal solution can be reached by making a locally optimal choice at each step. Usually proven with an exchange argument. (see [2.11](02-patterns/11-greedy/))

---

## H

**Hash function** — A function mapping a key to a bucket index (a number), ideally spreading keys evenly. A good one makes hash-table operations average `O(1)`; a bad one clusters keys and causes collisions. (see [1.2](01-data-structures/02-hashing/))

**Hash map** — A key-value store offering average `O(1)` insert, lookup, and delete via a hash function. Java's `HashMap` is the single most-used interview structure ("can you do better than O(n²)?" → "use a hash map"). (see [1.2](01-data-structures/02-hashing/))

**Hash set** — A collection of unique elements with average `O(1)` membership tests, built on the same hashing machinery as a hash map (it's a map with only keys). (see [1.2](01-data-structures/02-hashing/))

**hashCode** — Java's method returning an `int` used to pick a bucket in hash-based collections. Must be consistent with `equals` (see the equals/hashCode contract). (see [1.2](01-data-structures/02-hashing/))

**Heap** — See *Binary heap*. A tree-shaped structure that always gives quick access to its smallest or largest element. (see [1.6](01-data-structures/06-heaps/))

**Heapify** — Rearranging an array into valid heap order. Building a heap from `n` elements by sifting down is `O(n)` — faster than the `O(n log n)` you'd expect from `n` separate inserts. (see [1.6](01-data-structures/06-heaps/))

---

## I

**Immutability (string)** — In Java, a `String` cannot be changed after creation; every modification creates a new string. So concatenating in a loop with `+` is secretly `O(n²)` — use `StringBuilder` instead. (see [0.2](00-orientation/02-java-for-interviews.md))

**In-place** — Modifying the input data structure directly using only `O(1)` extra space, rather than allocating a copy. Common ask: "reverse the array in place." (see [1.1](01-data-structures/01-arrays-and-strings/))

**Integer cache** — Java caches `Integer` objects for values `−128..127`, so `==` *accidentally* works for small values but fails for larger ones. The cruelest bug — passes small tests, fails big ones. Always compare with `.equals()`. (see [0.2](00-orientation/02-java-for-interviews.md))

**Integer overflow** — When arithmetic exceeds a type's range and silently wraps around (in Java, `int` is 32-bit, so `2147483647 + 1` becomes negative — no exception). Guard sums, products, and midpoints; promote to `long`. (see [0.2](00-orientation/02-java-for-interviews.md))

**Interval** — A range with a start and end (`[2, 7]`), common in scheduling problems. Sorting intervals by start (or end) unlocks merging, overlap detection, and sweep-line techniques. (see [2.6](02-patterns/06-intervals/))

**Interval DP** — Dynamic programming where each subproblem is a contiguous range `[i, j]` and you combine sub-ranges (often by choosing a split point `k`). Used for matrix-chain multiplication, burst balloons, etc. (see [2.10](02-patterns/10-dp-advanced/))

**Inorder traversal** — A DFS tree traversal visiting left subtree, then node, then right subtree. On a BST it yields elements in *sorted* order — a frequently exploited fact. (see [1.5](01-data-structures/05-trees-and-bst/))

**Invariant** — A condition guaranteed true at a specific point throughout an algorithm (e.g. "the left portion is always sorted"). Identifying the invariant is the key to reasoning about loops and binary search correctness. (see [0.1](00-orientation/01-big-o-complexity.md))

**Inverse Ackermann** — A function `α(n)` that grows so slowly it's effectively a small constant (≤ 4) for any conceivable input. The amortized cost per union-find operation with both optimizations is `O(α(n))`. (see [1.9](01-data-structures/09-union-find/))

---

## K

**Kadane's algorithm** — A linear-time DP for the maximum-sum contiguous subarray: at each element, decide whether to extend the running sum or start fresh. The classic 1D DP starter. (see [2.8](02-patterns/08-dp-1d/))

**Knapsack (0/1)** — A DP where you choose a subset of items, each taken at most once, to maximize value under a weight budget. The template for "pick-or-skip with a capacity" problems. (see [2.10](02-patterns/10-dp-advanced/))

**Knapsack (unbounded)** — The knapsack variant where each item can be taken unlimited times (e.g. coin change). The DP transition reuses the same item rather than moving past it. (see [2.10](02-patterns/10-dp-advanced/))

**Kruskal** — A minimum-spanning-tree algorithm that sorts all edges by weight and adds the cheapest edge that doesn't form a cycle (using union-find to test cycles). `O(E log E)`. (see [2.13](02-patterns/13-graph-algorithms/))

---

## L

**Lazy propagation** — A segment-tree optimization that defers range updates by storing a pending change at a node and pushing it down only when needed. Makes range updates `O(log n)` instead of `O(n)`. (see [1.10](01-data-structures/10-advanced-trees/))

**LCS (longest common subsequence)** — The longest sequence of characters appearing in the same order (not necessarily contiguous) in two strings. A canonical 2D DP, `O(m·n)`; underlies diff tools. (see [2.10](02-patterns/10-dp-advanced/))

**Level-order traversal** — Visiting a tree level by level, top to bottom — exactly BFS on a tree, implemented with a queue. (see [1.5](01-data-structures/05-trees-and-bst/))

**LIFO (last-in, first-out)** — The ordering of a stack: the most recently added element is the first removed, like a stack of plates. (see [1.4](01-data-structures/04-stacks-and-queues/))

**Linked list** — A linear structure of nodes where each node holds a value and a reference to the next, so there's no contiguous block. `O(1)` insert/delete given a node, but `O(n)` to reach the `k`-th element (no random access). (see [1.3](01-data-structures/03-linked-lists/))

**LIS (longest increasing subsequence)** — The longest subsequence whose values strictly increase. Solvable in `O(n²)` DP or `O(n log n)` with binary search over a "tails" array. (see [2.8](02-patterns/08-dp-1d/))

**Load factor** — The ratio of stored entries to buckets in a hash table. When it crosses a threshold (0.75 in Java's `HashMap`), the table resizes and rehashes to keep operations near `O(1)`. (see [1.2](01-data-structures/02-hashing/))

**Logical shift** — A bit shift that fills vacated positions with zeros, ignoring sign. In Java, `>>>` is the unsigned (logical) right shift; `>>` is arithmetic (sign-preserving). (see [2.14](02-patterns/14-bit-manipulation/))

**Lower bound** — In a sorted array, the first position where a target could be inserted while keeping order — i.e. the first element `≥` target. Pairs with upper bound to find a value's range. (see [2.5](02-patterns/05-binary-search/))

---

## M

**Matrix** — A 2D grid of values (`int[][]` in Java, an array of arrays). Bounds-check `0 <= r < rows && 0 <= c < cols` before indexing — grid problems live and die on that guard. (see [2.15](02-patterns/15-math-matrix/))

**Max-heap** — A heap whose root is always the largest element. In Java, build one with `new PriorityQueue<>(Comparator.reverseOrder())` since the default is a min-heap. (see [1.6](01-data-structures/06-heaps/))

**Memoization** — Top-down DP: write the natural recursion, then cache each subproblem's answer (in a map or array) so repeated calls return instantly. Turns exponential recursion into polynomial time. (see [2.8](02-patterns/08-dp-1d/))

**Min-heap** — A heap whose root is always the smallest element. Java's `PriorityQueue` is a min-heap by default — a frequently forgotten fact. (see [1.6](01-data-structures/06-heaps/))

**Modular arithmetic** — Arithmetic that wraps around a modulus `m` (clock arithmetic). Used to keep huge DP/counting answers within range; in Java use `Math.floorMod` since `%` can return negatives. (see [2.15](02-patterns/15-math-matrix/))

**Monotonic predicate** — A yes/no test that, once it flips from false to true (or true to false), never flips back as the input grows. This monotonicity is exactly what makes binary search on the answer valid. (see [2.5](02-patterns/05-binary-search/))

**Monotonic stack** — A stack kept in sorted (increasing or decreasing) order by popping elements that violate the order as you push. The trick behind "next greater element" and "daily temperatures," giving `O(n)` total. (see [1.4](01-data-structures/04-stacks-and-queues/))

**MST (minimum spanning tree)** — The cheapest set of edges connecting all vertices of a weighted graph without cycles. Found with Prim's or Kruskal's algorithm. (see [2.13](02-patterns/13-graph-algorithms/))

---

## N

**Node** — A single element of a linked structure (list, tree, graph), bundling a value with one or more references to other nodes. (see [1.3](01-data-structures/03-linked-lists/))

---

## O

**Off-by-one error** — A bug from a boundary being one too far or one too short (e.g. `<=` vs `<`, or `n` vs `n-1`). The most common source of binary-search and loop mistakes; check endpoints deliberately. (see [2.5](02-patterns/05-binary-search/))

**Open addressing** — A collision-resolution scheme where, instead of a list per bucket, colliding keys probe for the next free slot in the array itself. Cache-friendly but sensitive to load factor. Contrast with separate chaining. (see [1.2](01-data-structures/02-hashing/))

**Optimal substructure** — The property that an optimal solution is built from optimal solutions to its subproblems. One of the two requirements (with overlapping subproblems) for dynamic programming to apply. (see [2.8](02-patterns/08-dp-1d/))

**Overlapping subproblems** — When a recursion solves the same subproblem many times (e.g. naive Fibonacci). Caching those repeated answers is what makes DP a speedup over plain recursion. (see [2.8](02-patterns/08-dp-1d/))

---

## P

**Path compression** — A union-find optimization that flattens the tree during `find` by pointing each visited node directly at the root, so future lookups are nearly `O(1)`. Pairs with union by rank. (see [1.9](01-data-structures/09-union-find/))

**Pointer / reference** — A value that "points at" where another object lives, rather than holding the object itself. Java has no raw pointers but uses references; reassigning a reference doesn't copy the object. (see [1.3](01-data-structures/03-linked-lists/))

**Postorder traversal** — A DFS tree traversal visiting left subtree, right subtree, then the node. Useful when a node's result depends on its children (e.g. computing subtree sizes or heights). (see [1.5](01-data-structures/05-trees-and-bst/))

**Prefix sum** — A precomputed array where entry `i` holds the sum of all elements up to `i`, letting you answer any range-sum query in `O(1)` after `O(n)` setup. The backbone of many subarray problems. (see [2.4](02-patterns/04-prefix-sum/))

**Prefix tree** — See *Trie*. (see [1.8](01-data-structures/08-tries/))

**Preorder traversal** — A DFS tree traversal visiting the node first, then left subtree, then right subtree. Useful for copying or serializing a tree top-down. (see [1.5](01-data-structures/05-trees-and-bst/))

**Prim** — A minimum-spanning-tree algorithm that grows the tree from one vertex, repeatedly adding the cheapest edge leaving the current tree (using a min-heap). `O(E log V)`. (see [2.13](02-patterns/13-graph-algorithms/))

**Priority queue** — An abstract queue where elements come out in priority order rather than insertion order, typically backed by a heap. Java's `PriorityQueue` is the standard implementation. (see [1.6](01-data-structures/06-heaps/))

**Pruning** — Cutting off branches of a search (backtracking, DFS) the moment they can't beat the best answer or can't satisfy a constraint. The difference between a feasible and a timed-out solution. (see [2.7](02-patterns/07-backtracking/))

---

## Q

**Queue** — A FIFO collection: add at the back, remove from the front. The engine of BFS; in Java use `ArrayDeque` with `offer`/`poll`/`peek`. (see [1.4](01-data-structures/04-stacks-and-queues/))

**Quickselect** — A selection algorithm that finds the `k`-th smallest element in average `O(n)` by partitioning like quicksort but recursing into only one side. Faster than fully sorting when you just need one rank. (see [2.12](02-patterns/12-heap-topk/))

---

## R

**Recurrence** — The mathematical relation expressing a problem's answer in terms of smaller instances (e.g. `dp[i] = dp[i-1] + dp[i-2]`). Writing the recurrence is the heart of designing a DP. (see [2.8](02-patterns/08-dp-1d/))

**Recursion** — A function that solves a problem by calling itself on smaller inputs until it hits a base case. Elegant for trees and divide-and-conquer, but each call uses a stack frame — watch the depth. (see [1.5](01-data-structures/05-trees-and-bst/))

---

## S

**Segment tree** — A binary tree over an array supporting `O(log n)` range queries (sum, min, max) *and* point/range updates. More powerful than a Fenwick tree but heavier to code. (see [1.10](01-data-structures/10-advanced-trees/))

**Sentinel** — A special placeholder value or node that simplifies edge cases — e.g. a dummy list head, or `Integer.MAX_VALUE` used as "infinity." (see [1.3](01-data-structures/03-linked-lists/))

**Separate chaining** — A collision-resolution scheme where each hash bucket holds a list (chain) of all keys that hashed there. Simple and robust; the strategy you'll implement when building a hash map. (see [1.2](01-data-structures/02-hashing/))

**Sieve of Eratosthenes** — An algorithm that finds all primes up to `n` by repeatedly crossing out multiples of each prime, in `O(n log log n)`. The standard way to precompute primes. (see [2.15](02-patterns/15-math-matrix/))

**Sift down (sift up)** — The two operations that restore heap order: sift up bubbles a newly inserted element toward the root; sift down pushes a displaced root down toward the leaves. Each is `O(log n)`. (see [1.6](01-data-structures/06-heaps/))

**Singly linked list** — A linked list where each node points only to the next node (no back-pointer). Lighter than a doubly linked list, but you can't step backward. (see [1.3](01-data-structures/03-linked-lists/))

**Skewed tree** — A degenerate tree where nodes form essentially a straight line (every node has one child). Its height is `O(n)`, so a skewed BST loses its `O(log n)` advantage and behaves like a linked list. (see [1.5](01-data-structures/05-trees-and-bst/))

**Sliding window** — A two-pointer technique that maintains a contiguous range (window) over an array/string, expanding and shrinking it to satisfy a constraint — turning many `O(n²)` scans into `O(n)`. Comes in fixed and variable forms. (see [2.2](02-patterns/02-sliding-window/))

**Sorting** — Arranging elements in order. General comparison sorts are `O(n log n)`; Java's `Arrays.sort` uses dual-pivot quicksort for primitives and a stable merge sort (Timsort) for objects. (see [2.6](02-patterns/06-intervals/))

**Space complexity** — How much extra memory an algorithm uses as a function of input size, in Big-O terms. Remember to count the recursion call stack and any auxiliary maps/arrays. (see [0.1](00-orientation/01-big-o-complexity.md))

**Stable sort** — A sort that preserves the relative order of elements comparing equal. Matters when sorting by one key after another (multi-level sorting); Java's object sort is stable, its primitive sort is not. (see [2.6](02-patterns/06-intervals/))

**Stack** — A LIFO collection: push and pop from the same end. Powers DFS, expression parsing, and undo. In Java prefer `ArrayDeque` over the legacy `Stack` class. (see [1.4](01-data-structures/04-stacks-and-queues/))

**StackOverflowError** — Java's error thrown when recursion exceeds the call stack (~10k–20k frames). Triggered by missing base cases or recursing on very deep inputs — convert to iteration with an explicit stack when depth can be large. (see [0.2](00-orientation/02-java-for-interviews.md))

**State** — The set of variables that fully describe a DP subproblem (e.g. "index `i` and remaining capacity `w`"). Choosing the right state is the make-or-break step of DP design. (see [2.8](02-patterns/08-dp-1d/))

**StringBuilder** — Java's mutable string buffer for building strings in `O(n)` total. Use it instead of `+=` in loops, which is `O(n²)` because strings are immutable. (see [0.2](00-orientation/02-java-for-interviews.md))

**Sweep line** — A technique that processes interval/geometry events in sorted order along an axis, maintaining a running structure as an imaginary line "sweeps" across. Used for overlapping intervals, meeting rooms, skyline. (see [2.6](02-patterns/06-intervals/))

---

## T

**Tabulation** — Bottom-up DP: fill a table from the smallest subproblems upward, iteratively, with no recursion. Avoids stack-depth limits and is often slightly faster than memoization. (see [2.8](02-patterns/08-dp-1d/))

**Time complexity** — How an algorithm's running time grows with input size, in Big-O terms. The number interviewers always ask for — state it out loud alongside space. (see [0.1](00-orientation/01-big-o-complexity.md))

**Topological sort** — A linear ordering of a DAG's vertices such that every edge points forward (every prerequisite comes before what depends on it). Computed via DFS or Kahn's BFS algorithm; impossible if there's a cycle. (see [2.13](02-patterns/13-graph-algorithms/))

**Transition** — The rule in DP that computes one state's answer from already-known states (the body of the recurrence). "Given the smaller answers, how do I build this one?" (see [2.8](02-patterns/08-dp-1d/))

**Traversal** — Visiting every node of a structure in some defined order. For trees: preorder, inorder, postorder (all DFS), and level-order (BFS). (see [1.5](01-data-structures/05-trees-and-bst/))

**Tree** — A connected, acyclic graph with a root, where every node has children and exactly one parent (except the root). Models hierarchies: file systems, org charts, parse trees. (see [1.5](01-data-structures/05-trees-and-bst/))

**Trie (prefix tree)** — A tree where each path from the root spells out a string, so shared prefixes share nodes. Gives `O(L)` insert/lookup for a length-`L` word and powers autocomplete and prefix search. (see [1.8](01-data-structures/08-tries/))

**Two pointers** — A technique using two index pointers moving through an array/string (toward each other, or one chasing the other) to solve in `O(n)` what brute force does in `O(n²)`. (see [2.1](02-patterns/01-two-pointers/))

**Two's complement** — The standard binary representation of signed integers, where the top bit signals negativity and negation is "flip all bits, add one." Explains why `int` overflow wraps from max to min. (see [2.14](02-patterns/14-bit-manipulation/))

---

## U

**Undirected graph** — A graph whose edges have no direction; if A connects to B, B connects to A. Models mutual relationships like friendships or roads. (see [1.7](01-data-structures/07-graphs/))

**Union by rank** — A union-find optimization that, when merging two sets, attaches the shorter tree under the taller one to keep trees shallow. Pairs with path compression for near-constant operations. (see [1.9](01-data-structures/09-union-find/))

**Union-Find (DSU)** — A structure tracking a partition of elements into disjoint sets, with near-`O(1)` `union` (merge two sets) and `find` (which set?) operations. The tool for connectivity and cycle problems. (see [1.9](01-data-structures/09-union-find/))

**Upper bound** — In a sorted array, the first position past all elements equal to the target — i.e. the first element strictly `>` target. Together with lower bound it brackets a value's occurrences. (see [2.5](02-patterns/05-binary-search/))

---

## V

**Variable-size sliding window** — A sliding-window variant where the window grows and shrinks to satisfy a constraint (e.g. "longest substring with ≤ k distinct chars"). The right edge expands; the left edge contracts when the constraint breaks. (see [2.2](02-patterns/02-sliding-window/))

**Vertex** — A node in a graph — the things edges connect. Plural: vertices; denoted `V` in complexity bounds like `O(V + E)`. (see [1.7](01-data-structures/07-graphs/))

---

## W

**Weighted graph** — A graph whose edges carry numeric costs (distance, time, capacity). Shortest-path and MST algorithms operate on these. (see [1.7](01-data-structures/07-graphs/))

**Worst case** — The slowest an algorithm can run over all possible inputs — the bound interviewers care about most because it guarantees behavior under adversarial input. (see [0.1](00-orientation/01-big-o-complexity.md))

---

## X

**XOR trick** — Exploiting that `a ^ a = 0` and `a ^ 0 = a`, so XOR-ing a list cancels every value that appears an even number of times — instantly finding the lone unpaired element in `O(n)` time and `O(1)` space. (see [2.14](02-patterns/14-bit-manipulation/))
