# DSA Interview Questions -- Top Tech Companies (SDET / SDE Roles)

> Comprehensive, topic-wise list of the most frequently asked Data Structures & Algorithms
> problems at Google, Microsoft, Apple, Amazon, Meta, Netflix, Disney+ Hotstar, and other
> top product companies. Curated from LeetCode frequency data, Blind 75, NeetCode 150,
> company-tagged question lists, and verified interview experiences (2024-2026).

---

## How to Use This Guide

| Label | Meaning |
|-------|---------|
| **MUST-DO** | Appears repeatedly across multiple companies; tested pattern you will almost certainly encounter. |
| **GOOD-TO-KNOW** | Asked at specific companies or tests a useful secondary pattern; do these after covering all MUST-DOs. |

**Difficulty key:** E = Easy, M = Medium, H = Hard

---

## Topic Importance Ranking (by interview frequency)

| Rank | Topic | Frequency in FAANG Interviews |
|------|-------|-------------------------------|
| 1 | Arrays | Very High -- warm-up in almost every interview |
| 2 | Strings | Very High -- paired with arrays at most companies |
| 3 | HashMap / HashSet | Very High -- fundamental to 60%+ of optimal solutions |
| 4 | Trees (Binary Tree, BST) | High -- 20-30% of FAANG coding rounds |
| 5 | Graphs | High -- BFS/DFS appear in most on-sites |
| 6 | Dynamic Programming | High -- Google, Amazon, Microsoft favor DP heavily |
| 7 | Two Pointers | High -- pattern behind many array/string problems |
| 8 | Sliding Window | High -- one of the most tested patterns |
| 9 | Binary Search | High -- tested directly or as part of harder problems |
| 10 | Linked Lists | Medium-High -- at least one round at most companies |
| 11 | Stacks | Medium-High -- often combined with string/parsing problems |
| 12 | Heaps / Priority Queues | Medium -- Top-K and merge problems |
| 13 | Sorting | Medium -- often a building block for other solutions |
| 14 | Greedy | Medium -- Amazon and Google ask these regularly |
| 15 | Backtracking | Medium -- permutations/combinations appear frequently |
| 16 | Queues | Medium -- BFS foundation; design problems |
| 17 | Matrix | Medium -- variant of graph/array problems |
| 18 | Recursion | Medium -- foundational; tested implicitly in trees/DP/backtracking |

---

## 1. Arrays

> **Overall importance: VERY HIGH**
> Arrays are the workhorses of every coding interview. Google, Meta, and Amazon use them as warm-ups because they reveal how you handle logic, edge cases, and optimization under pressure.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Two Sum | 1 | E | Amazon, Microsoft, Google, Apple, Meta, Bloomberg | **MUST-DO** |
| 2 | Best Time to Buy and Sell Stock | 121 | E | Amazon, Meta, Microsoft, Goldman Sachs, Apple | **MUST-DO** |
| 3 | Contains Duplicate | 217 | E | Amazon, Apple, Microsoft, Adobe | **MUST-DO** |
| 4 | Product of Array Except Self | 238 | M | Amazon, Meta, Apple, Microsoft, Google | **MUST-DO** |
| 5 | Maximum Subarray (Kadane's) | 53 | M | Amazon, Microsoft, Google, Apple, LinkedIn | **MUST-DO** |
| 6 | Merge Intervals | 56 | M | Amazon, Meta, Google, Microsoft, Stripe, Apple | **MUST-DO** |
| 7 | 3Sum | 15 | M | Meta, Amazon, Google, Microsoft, Apple, Bloomberg | **MUST-DO** |
| 8 | Container With Most Water | 11 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 9 | Insert Interval | 57 | M | Google, Meta, Amazon, LinkedIn | **MUST-DO** |
| 10 | Maximum Product Subarray | 152 | M | Amazon, Google, Microsoft, LinkedIn | **MUST-DO** |
| 11 | Find Minimum in Rotated Sorted Array | 153 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 12 | Search in Rotated Sorted Array | 33 | M | Amazon, Microsoft, Google, Meta, Apple | **MUST-DO** |
| 13 | Next Permutation | 31 | M | Google, Amazon, Microsoft, Meta | GOOD-TO-KNOW |
| 14 | Trapping Rain Water | 42 | H | Google, Amazon, Meta, Microsoft, Apple, Goldman Sachs | **MUST-DO** |
| 15 | First Missing Positive | 41 | H | Amazon, Google, Microsoft | GOOD-TO-KNOW |

---

## 2. Strings

> **Overall importance: VERY HIGH**
> Strings test pattern recognition, edge-case handling, and familiarity with hash maps and two-pointer techniques. Meta and Amazon especially love string problems.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Valid Anagram | 242 | E | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 2 | Valid Palindrome | 125 | E | Meta, Microsoft, Apple | **MUST-DO** |
| 3 | Longest Substring Without Repeating Characters | 3 | M | Amazon, Meta, Google, Microsoft, Netflix, Apple, Bloomberg | **MUST-DO** |
| 4 | Longest Palindromic Substring | 5 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 5 | Group Anagrams | 49 | M | Google, Amazon, Meta, Microsoft, Bloomberg, Citadel | **MUST-DO** |
| 6 | Longest Repeating Character Replacement | 424 | M | Google, Amazon, Microsoft | **MUST-DO** |
| 7 | String to Integer (atoi) | 8 | M | Amazon, Microsoft, Apple, Meta | GOOD-TO-KNOW |
| 8 | Generate Parentheses | 22 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 9 | Palindromic Substrings | 647 | M | Amazon, Meta, Google | GOOD-TO-KNOW |
| 10 | Encode and Decode Strings | 271 | M | Google, Meta, Amazon | **MUST-DO** |
| 11 | Minimum Window Substring | 76 | H | Meta, Amazon, Google, Microsoft, Apple, LinkedIn | **MUST-DO** |
| 12 | Minimum Remove to Make Valid Parentheses | 1249 | M | Meta, Amazon, Microsoft | **MUST-DO** |
| 13 | Valid Parentheses | 20 | E | Amazon, Meta, Google, Microsoft, Apple, Bloomberg | **MUST-DO** |
| 14 | Longest Common Prefix | 14 | E | Amazon, Apple, Google | GOOD-TO-KNOW |
| 15 | Edit Distance | 72 | H | Google, Amazon, Dropbox, Microsoft | GOOD-TO-KNOW |

---

## 3. HashMap / HashSet

> **Overall importance: VERY HIGH**
> Hash-based structures provide O(1) lookups and are the backbone of optimal solutions in 60%+ of interview problems. Design problems like LRU Cache are perennial favorites.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Two Sum | 1 | E | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 2 | Valid Anagram | 242 | E | Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 3 | Group Anagrams | 49 | M | Google, Amazon, Meta, Microsoft, Bloomberg, Citadel | **MUST-DO** |
| 4 | Top K Frequent Elements | 347 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 5 | Longest Consecutive Sequence | 128 | M | Google, Amazon, Meta, Microsoft | **MUST-DO** |
| 6 | Subarray Sum Equals K | 560 | M | Meta, Google, Amazon, Microsoft | **MUST-DO** |
| 7 | LRU Cache | 146 | M | Amazon, Meta, Google, Microsoft, Apple, Netflix | **MUST-DO** |
| 8 | Copy List with Random Pointer | 138 | M | Amazon, Microsoft, Meta | **MUST-DO** |
| 9 | Contains Duplicate II | 219 | E | Amazon, Google | GOOD-TO-KNOW |
| 10 | Isomorphic Strings | 205 | E | Amazon, Google, Microsoft | GOOD-TO-KNOW |
| 11 | Word Pattern | 290 | E | Amazon, Microsoft | GOOD-TO-KNOW |
| 12 | Happy Number | 202 | E | Amazon, Apple | GOOD-TO-KNOW |
| 13 | 4Sum II | 454 | M | Amazon, Google | GOOD-TO-KNOW |
| 14 | Minimum Window Substring | 76 | H | Meta, Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 15 | Design HashMap | 706 | E | Amazon, Apple, Goldman Sachs | GOOD-TO-KNOW |

---

## 4. Linked Lists

> **Overall importance: MEDIUM-HIGH**
> At least one linked list question appears in most company interview loops. Focus on pointer manipulation, cycle detection, and merge patterns.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Reverse Linked List | 206 | E | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 2 | Merge Two Sorted Lists | 21 | E | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 3 | Linked List Cycle | 141 | E | Amazon, Microsoft, Apple, Meta | **MUST-DO** |
| 4 | Remove Nth Node From End of List | 19 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 5 | Add Two Numbers | 2 | M | Amazon, Microsoft, Google, Meta, Apple | **MUST-DO** |
| 6 | Reorder List | 143 | M | Amazon, Meta, Microsoft | **MUST-DO** |
| 7 | Linked List Cycle II | 142 | M | Amazon, Microsoft, Apple | **MUST-DO** |
| 8 | Copy List with Random Pointer | 138 | M | Amazon, Microsoft, Meta | **MUST-DO** |
| 9 | LRU Cache | 146 | M | Amazon, Meta, Google, Microsoft, Apple, Netflix | **MUST-DO** |
| 10 | Flatten a Multilevel Doubly Linked List | 430 | M | Microsoft, Amazon | GOOD-TO-KNOW |
| 11 | Sort List | 148 | M | Amazon, Microsoft, Google | GOOD-TO-KNOW |
| 12 | Merge k Sorted Lists | 23 | H | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 13 | Reverse Nodes in k-Group | 25 | H | Amazon, Meta, Google, Microsoft | GOOD-TO-KNOW |
| 14 | Palindrome Linked List | 234 | E | Amazon, Meta, Microsoft | GOOD-TO-KNOW |
| 15 | Intersection of Two Linked Lists | 160 | E | Amazon, Microsoft, Meta | GOOD-TO-KNOW |

---

## 5. Stacks

> **Overall importance: MEDIUM-HIGH**
> Stack problems test your ability to handle LIFO ordering, nested structures, and monotonic patterns. Valid Parentheses alone accounts for a huge number of interview appearances.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Valid Parentheses | 20 | E | Amazon, Meta, Google, Microsoft, Apple, Bloomberg | **MUST-DO** |
| 2 | Min Stack | 155 | M | Amazon, Microsoft, Google, Apple, Bloomberg | **MUST-DO** |
| 3 | Evaluate Reverse Polish Notation | 150 | M | Amazon, Google, Microsoft, LinkedIn | **MUST-DO** |
| 4 | Daily Temperatures | 739 | M | Amazon, Google, Meta | **MUST-DO** |
| 5 | Next Greater Element I | 496 | E | Amazon, Bloomberg | GOOD-TO-KNOW |
| 6 | Decode String | 394 | M | Google, Amazon, Microsoft, Apple | **MUST-DO** |
| 7 | Largest Rectangle in Histogram | 84 | H | Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 8 | Basic Calculator II | 227 | M | Meta, Amazon, Microsoft | **MUST-DO** |
| 9 | Asteroid Collision | 735 | M | Amazon, Google | GOOD-TO-KNOW |
| 10 | Implement Stack using Queues | 225 | E | Amazon, Microsoft, Apple | GOOD-TO-KNOW |
| 11 | Trapping Rain Water | 42 | H | Google, Amazon, Meta, Microsoft, Apple | **MUST-DO** |
| 12 | Car Fleet | 853 | M | Google, Amazon | GOOD-TO-KNOW |
| 13 | Online Stock Span | 901 | M | Amazon, Disney+ Hotstar | GOOD-TO-KNOW |
| 14 | Remove All Adjacent Duplicates in String II | 1209 | M | Amazon, Microsoft | GOOD-TO-KNOW |
| 15 | Minimum Remove to Make Valid Parentheses | 1249 | M | Meta, Amazon, Microsoft | **MUST-DO** |

---

## 6. Queues

> **Overall importance: MEDIUM**
> Queues are the foundation for BFS traversal. Design problems (implement queue with stacks) and sliding window maximums are classic interview favorites.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Implement Queue using Stacks | 232 | E | Amazon, Microsoft, Apple | **MUST-DO** |
| 2 | Number of Islands (BFS approach) | 200 | M | Amazon, Google, Meta, Microsoft, Apple | **MUST-DO** |
| 3 | Binary Tree Level Order Traversal | 102 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 4 | Rotting Oranges | 994 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 5 | Walls and Gates | 286 | M | Meta, Google, Amazon | **MUST-DO** |
| 6 | Design Hit Counter | 362 | M | Amazon, Google, Meta | **MUST-DO** |
| 7 | Sliding Window Maximum | 239 | H | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 8 | Task Scheduler | 621 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 9 | Open the Lock | 752 | M | Google, Amazon | GOOD-TO-KNOW |
| 10 | Design Circular Queue | 622 | M | Amazon, Microsoft | GOOD-TO-KNOW |
| 11 | Shortest Path in Binary Matrix | 1091 | M | Meta, Amazon, Google | GOOD-TO-KNOW |
| 12 | Perfect Squares | 279 | M | Google, Amazon | GOOD-TO-KNOW |

---

## 7. Trees (Binary Tree, BST)

> **Overall importance: HIGH**
> Medium-hard tree problems appear in 20-30% of FAANG coding sessions. Interviewers often ask for both recursive and iterative solutions. LCA problems are perennial favorites.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Maximum Depth of Binary Tree | 104 | E | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 2 | Invert Binary Tree | 226 | E | Google, Amazon, Meta, Apple, Microsoft | **MUST-DO** |
| 3 | Same Tree | 100 | E | Amazon, Microsoft, Apple | **MUST-DO** |
| 4 | Symmetric Tree | 101 | E | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 5 | Binary Tree Level Order Traversal | 102 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 6 | Validate Binary Search Tree | 98 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 7 | Kth Smallest Element in a BST | 230 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 8 | Lowest Common Ancestor of a Binary Tree | 236 | M | Meta, Amazon, Google, Microsoft, Apple, LinkedIn | **MUST-DO** |
| 9 | Binary Tree Right Side View | 199 | M | Meta, Amazon, Google, Microsoft | **MUST-DO** |
| 10 | Construct Binary Tree from Preorder and Inorder Traversal | 105 | M | Amazon, Google, Microsoft, Meta | **MUST-DO** |
| 11 | Diameter of Binary Tree | 543 | E | Meta, Amazon, Google, Salesforce | **MUST-DO** |
| 12 | Convert Sorted Array to BST | 108 | E | Amazon, Microsoft, Google | **MUST-DO** |
| 13 | Binary Tree Zigzag Level Order Traversal | 103 | M | Amazon, Meta, Microsoft | GOOD-TO-KNOW |
| 14 | Flatten Binary Tree to Linked List | 114 | M | Amazon, Meta, Microsoft | GOOD-TO-KNOW |
| 15 | Serialize and Deserialize Binary Tree | 297 | H | Amazon, Meta, Google, Microsoft, Apple, LinkedIn | **MUST-DO** |

---

## 8. Graphs

> **Overall importance: HIGH**
> DFS, BFS, Union Find, and Topological Sort cover the vast majority of graph questions. Number of Islands alone has been asked at almost every major company.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Number of Islands | 200 | M | Amazon, Google, Meta, Microsoft, Apple, LinkedIn, Bloomberg | **MUST-DO** |
| 2 | Clone Graph | 133 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 3 | Course Schedule | 207 | M | Amazon, Google, Meta, Microsoft, Apple | **MUST-DO** |
| 4 | Course Schedule II | 210 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 5 | Pacific Atlantic Water Flow | 417 | M | Google, Amazon, Meta | **MUST-DO** |
| 6 | Number of Connected Components in Undirected Graph | 323 | M | Google, Amazon, Meta, Microsoft | **MUST-DO** |
| 7 | Graph Valid Tree | 261 | M | Google, Amazon, Meta | **MUST-DO** |
| 8 | Word Ladder | 127 | H | Google, Amazon, Meta, LinkedIn | **MUST-DO** |
| 9 | Alien Dictionary | 269 | H | Google, Amazon, Meta, Microsoft, Apple | **MUST-DO** |
| 10 | Accounts Merge | 721 | M | Meta, Amazon, Google | **MUST-DO** |
| 11 | Cheapest Flights Within K Stops | 787 | M | Amazon, Google, Meta | GOOD-TO-KNOW |
| 12 | Network Delay Time | 743 | M | Google, Amazon | GOOD-TO-KNOW |
| 13 | Redundant Connection | 684 | M | Google, Amazon | GOOD-TO-KNOW |
| 14 | Surrounded Regions | 130 | M | Amazon, Google, Microsoft | GOOD-TO-KNOW |
| 15 | Shortest Path in Binary Matrix | 1091 | M | Meta, Amazon, Google | GOOD-TO-KNOW |

---

## 9. Heaps / Priority Queues

> **Overall importance: MEDIUM**
> Heap problems often appear as "Top K" or "merge K sorted" patterns. Kth Largest Element is one of the most frequently asked problems across all companies.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Kth Largest Element in an Array | 215 | M | Amazon, Meta, Google, Microsoft, Apple, Uber | **MUST-DO** |
| 2 | Top K Frequent Elements | 347 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 3 | Merge k Sorted Lists | 23 | H | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 4 | Find Median from Data Stream | 295 | H | Amazon, Google, Meta, Microsoft, Apple | **MUST-DO** |
| 5 | Task Scheduler | 621 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 6 | K Closest Points to Origin | 973 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 7 | Reorganize String | 767 | M | Amazon, Google, Meta | GOOD-TO-KNOW |
| 8 | Kth Smallest Element in a Sorted Matrix | 378 | M | Amazon, Google, Microsoft | GOOD-TO-KNOW |
| 9 | Meeting Rooms II | 253 | M | Amazon, Meta, Google, Microsoft, Bloomberg | **MUST-DO** |
| 10 | Sort Characters By Frequency | 451 | M | Amazon, Google, Bloomberg | GOOD-TO-KNOW |
| 11 | Last Stone Weight | 1046 | E | Amazon, Google | GOOD-TO-KNOW |
| 12 | Design Twitter | 355 | M | Amazon, Google | GOOD-TO-KNOW |

---

## 10. Binary Search

> **Overall importance: HIGH**
> Binary search is tested directly and also as a sub-routine in harder problems. "Binary Search on Answer" is a trending pattern in 2025-2026 interviews.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Binary Search | 704 | E | Amazon, Microsoft, Google | **MUST-DO** |
| 2 | Search in Rotated Sorted Array | 33 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 3 | Find Minimum in Rotated Sorted Array | 153 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 4 | Search a 2D Matrix | 74 | M | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 5 | Find Peak Element | 162 | M | Google, Amazon, Meta, Microsoft | **MUST-DO** |
| 6 | Koko Eating Bananas | 875 | M | Google, Amazon | **MUST-DO** |
| 7 | Time Based Key-Value Store | 981 | M | Google, Amazon, Meta | **MUST-DO** |
| 8 | Median of Two Sorted Arrays | 4 | H | Amazon, Google, Microsoft, Apple, Meta, Goldman Sachs | **MUST-DO** |
| 9 | Find First and Last Position of Element in Sorted Array | 34 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 10 | Capacity To Ship Packages Within D Days | 1011 | M | Amazon, Google | GOOD-TO-KNOW |
| 11 | Split Array Largest Sum | 410 | H | Google, Amazon | GOOD-TO-KNOW |
| 12 | Search a 2D Matrix II | 240 | M | Amazon, Microsoft, Google, Apple | GOOD-TO-KNOW |

---

## 11. Sliding Window

> **Overall importance: HIGH**
> One of the highest-ROI patterns. A handful of templates cover nearly all sliding window problems. Meta and Amazon especially favor this pattern.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Best Time to Buy and Sell Stock | 121 | E | Amazon, Meta, Microsoft, Goldman Sachs, Apple | **MUST-DO** |
| 2 | Longest Substring Without Repeating Characters | 3 | M | Amazon, Meta, Google, Microsoft, Netflix, Apple | **MUST-DO** |
| 3 | Longest Repeating Character Replacement | 424 | M | Google, Amazon, Microsoft | **MUST-DO** |
| 4 | Minimum Window Substring | 76 | H | Meta, Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 5 | Permutation in String | 567 | M | Amazon, Microsoft, Google | **MUST-DO** |
| 6 | Sliding Window Maximum | 239 | H | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 7 | Minimum Size Subarray Sum | 209 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 8 | Fruit Into Baskets | 904 | M | Google, Amazon | GOOD-TO-KNOW |
| 9 | Subarrays with K Different Integers | 992 | H | Amazon, Google | GOOD-TO-KNOW |
| 10 | Maximum Number of Vowels in a Substring of Given Length | 1456 | M | Amazon | GOOD-TO-KNOW |
| 11 | Substring with Concatenation of All Words | 30 | H | Amazon, Google | GOOD-TO-KNOW |
| 12 | Find All Anagrams in a String | 438 | M | Amazon, Meta, Microsoft | **MUST-DO** |

---

## 12. Two Pointers

> **Overall importance: HIGH**
> Two pointers is the pattern behind many array and string problems. Mastering this technique is essential before tackling more advanced topics.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Two Sum II - Input Array Is Sorted | 167 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 2 | 3Sum | 15 | M | Meta, Amazon, Google, Microsoft, Apple, Bloomberg | **MUST-DO** |
| 3 | Container With Most Water | 11 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 4 | Trapping Rain Water | 42 | H | Google, Amazon, Meta, Microsoft, Apple, Goldman Sachs | **MUST-DO** |
| 5 | Valid Palindrome | 125 | E | Meta, Microsoft, Apple | **MUST-DO** |
| 6 | Remove Duplicates from Sorted Array | 26 | E | Amazon, Microsoft, Google | **MUST-DO** |
| 7 | Move Zeroes | 283 | E | Meta, Amazon, Microsoft, Apple | **MUST-DO** |
| 8 | Sort Colors (Dutch National Flag) | 75 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 9 | Merge Sorted Array | 88 | E | Amazon, Meta, Microsoft | **MUST-DO** |
| 10 | Linked List Cycle | 141 | E | Amazon, Microsoft, Apple, Meta | **MUST-DO** |
| 11 | 4Sum | 18 | M | Amazon, Google, Microsoft | GOOD-TO-KNOW |
| 12 | Remove Duplicates from Sorted Array II | 80 | M | Amazon, Microsoft | GOOD-TO-KNOW |
| 13 | Palindrome Linked List | 234 | E | Amazon, Meta, Microsoft | GOOD-TO-KNOW |
| 14 | Boats to Save People | 881 | M | Google, Amazon | GOOD-TO-KNOW |

---

## 13. Dynamic Programming

> **Overall importance: HIGH**
> Google, Amazon, and Microsoft heavily favor DP. Meta has moved away from DP in recent years, but it still appears. Master the core patterns: 1D DP, 2D DP, knapsack, LIS, interval DP.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Climbing Stairs | 70 | E | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 2 | House Robber | 198 | M | Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 3 | House Robber II | 213 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 4 | Coin Change | 322 | M | Amazon, Google, Microsoft, Apple, Meta | **MUST-DO** |
| 5 | Longest Increasing Subsequence | 300 | M | Amazon, Google, Microsoft, Meta | **MUST-DO** |
| 6 | Word Break | 139 | M | Amazon, Google, Meta, Microsoft, Apple | **MUST-DO** |
| 7 | Unique Paths | 62 | M | Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 8 | Longest Common Subsequence | 1143 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 9 | Decode Ways | 91 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 10 | Jump Game | 55 | M | Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 11 | Jump Game II | 45 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 12 | Partition Equal Subset Sum (0/1 Knapsack) | 416 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 13 | Longest Palindromic Substring | 5 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 14 | Edit Distance | 72 | H | Google, Amazon, Dropbox, Microsoft | GOOD-TO-KNOW |
| 15 | Regular Expression Matching | 10 | H | Google, Amazon, Meta | GOOD-TO-KNOW |

---

## 14. Backtracking

> **Overall importance: MEDIUM**
> Backtracking tests your recursion fundamentals and ability to prune search spaces. Subsets, Permutations, and Combination Sum are the classic trio.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Subsets | 78 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 2 | Permutations | 46 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 3 | Combination Sum | 39 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 4 | Combination Sum II | 40 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 5 | Letter Combinations of a Phone Number | 17 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 6 | Word Search | 79 | M | Amazon, Google, Meta, Microsoft, Apple | **MUST-DO** |
| 7 | Palindrome Partitioning | 131 | M | Amazon, Google, Meta | GOOD-TO-KNOW |
| 8 | Subsets II | 90 | M | Amazon, Meta, Google | GOOD-TO-KNOW |
| 9 | Permutations II | 47 | M | Amazon, Google, Meta | GOOD-TO-KNOW |
| 10 | N-Queens | 51 | H | Amazon, Google, Microsoft, Apple | GOOD-TO-KNOW |
| 11 | Sudoku Solver | 37 | H | Amazon, Google, Microsoft | GOOD-TO-KNOW |
| 12 | Generate Parentheses | 22 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 13 | Restore IP Addresses | 93 | M | Amazon, Google | GOOD-TO-KNOW |
| 14 | Word Search II | 212 | H | Amazon, Google, Microsoft | GOOD-TO-KNOW |

---

## 15. Greedy

> **Overall importance: MEDIUM**
> Greedy problems test whether you can identify when the locally optimal choice leads to the globally optimal solution. Amazon and Google ask these regularly.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Jump Game | 55 | M | Amazon, Google, Microsoft, Apple | **MUST-DO** |
| 2 | Jump Game II | 45 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 3 | Best Time to Buy and Sell Stock II | 122 | M | Amazon, Meta, Microsoft, Goldman Sachs | **MUST-DO** |
| 4 | Gas Station | 134 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 5 | Maximum Subarray (Kadane's) | 53 | M | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 6 | Hand of Straights | 846 | M | Google, Amazon | GOOD-TO-KNOW |
| 7 | Merge Triplets to Form Target Triplet | 1899 | M | Google | GOOD-TO-KNOW |
| 8 | Partition Labels | 763 | M | Amazon, Google | **MUST-DO** |
| 9 | Valid Parenthesis String | 678 | M | Amazon, Google | GOOD-TO-KNOW |
| 10 | Meeting Rooms II | 253 | M | Amazon, Meta, Google, Microsoft, Bloomberg | **MUST-DO** |
| 11 | Non-overlapping Intervals | 435 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 12 | Minimum Number of Arrows to Burst Balloons | 452 | M | Amazon, Google | GOOD-TO-KNOW |
| 13 | Task Scheduler | 621 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |

---

## 16. Sorting

> **Overall importance: MEDIUM**
> Sorting is often a prerequisite for other techniques (two pointers, greedy, intervals). Understanding merge sort and quick sort internals is expected.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Merge Intervals | 56 | M | Amazon, Meta, Google, Microsoft, Stripe, Apple | **MUST-DO** |
| 2 | Sort Colors (Dutch National Flag) | 75 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 3 | Kth Largest Element in an Array | 215 | M | Amazon, Meta, Google, Microsoft, Uber | **MUST-DO** |
| 4 | Meeting Rooms | 252 | E | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 5 | Meeting Rooms II | 253 | M | Amazon, Meta, Google, Microsoft, Bloomberg | **MUST-DO** |
| 6 | Largest Number | 179 | M | Amazon, Google, Meta | GOOD-TO-KNOW |
| 7 | Sort List | 148 | M | Amazon, Microsoft, Google | GOOD-TO-KNOW |
| 8 | Merge Sorted Array | 88 | E | Amazon, Meta, Microsoft | **MUST-DO** |
| 9 | Insert Interval | 57 | M | Google, Meta, Amazon, LinkedIn | **MUST-DO** |
| 10 | Top K Frequent Elements | 347 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 11 | Valid Anagram | 242 | E | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 12 | K Closest Points to Origin | 973 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |

---

## 17. Matrix

> **Overall importance: MEDIUM**
> Matrix problems are essentially 2D array / graph problems. Spiral traversal, rotation, and island-counting are the most common patterns.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Number of Islands | 200 | M | Amazon, Google, Meta, Microsoft, Apple, Bloomberg | **MUST-DO** |
| 2 | Set Matrix Zeroes | 73 | M | Amazon, Microsoft, Google, Meta | **MUST-DO** |
| 3 | Spiral Matrix | 54 | M | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 4 | Rotate Image | 48 | M | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 5 | Search a 2D Matrix | 74 | M | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 6 | Word Search | 79 | M | Amazon, Google, Meta, Microsoft, Apple | **MUST-DO** |
| 7 | Valid Sudoku | 36 | M | Amazon, Microsoft, Apple | GOOD-TO-KNOW |
| 8 | Surrounded Regions | 130 | M | Amazon, Google, Microsoft | GOOD-TO-KNOW |
| 9 | Maximal Square | 221 | M | Amazon, Google, Microsoft | **MUST-DO** |
| 10 | Game of Life | 289 | M | Google, Amazon, Microsoft | GOOD-TO-KNOW |
| 11 | Shortest Path in Binary Matrix | 1091 | M | Meta, Amazon, Google | GOOD-TO-KNOW |
| 12 | Kth Smallest Element in a Sorted Matrix | 378 | M | Amazon, Google, Microsoft | GOOD-TO-KNOW |

---

## 18. Recursion

> **Overall importance: MEDIUM**
> Recursion is the foundation for trees, graphs, DP, and backtracking. These problems test pure recursive thinking and the ability to define base cases and recurrence relations.

| # | Problem | LC # | Diff | Companies | Priority |
|---|---------|------|------|-----------|----------|
| 1 | Fibonacci Number | 509 | E | Amazon, Microsoft, Google | **MUST-DO** |
| 2 | Pow(x, n) | 50 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 3 | Merge Two Sorted Lists | 21 | E | Amazon, Microsoft, Google, Apple | **MUST-DO** |
| 4 | Reverse Linked List | 206 | E | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 5 | Maximum Depth of Binary Tree | 104 | E | Amazon, Microsoft, Google, Apple, Meta | **MUST-DO** |
| 6 | Generate Parentheses | 22 | M | Amazon, Google, Meta, Microsoft | **MUST-DO** |
| 7 | Letter Combinations of a Phone Number | 17 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 8 | Subsets | 78 | M | Amazon, Meta, Google, Microsoft | **MUST-DO** |
| 9 | Permutations | 46 | M | Amazon, Meta, Google, Microsoft, Apple | **MUST-DO** |
| 10 | Flatten Nested List Iterator | 341 | M | Amazon, Google, Meta | GOOD-TO-KNOW |
| 11 | K-th Symbol in Grammar | 779 | M | Amazon, Google | GOOD-TO-KNOW |
| 12 | Tower of Hanoi (classic) | -- | M | Microsoft, Amazon, Disney+ Hotstar | **MUST-DO** |
| 13 | String Permutations (classic) | -- | M | All companies | **MUST-DO** |
| 14 | Climbing Stairs | 70 | E | Amazon, Microsoft, Google, Apple | **MUST-DO** |

---

## Company-Specific Highlights

### Google
- Heavy focus on **graphs** (topological sort, shortest paths), **DP**, and **binary search on answer**.
- Frequently asks hard problems; expects optimal solutions with trade-off discussions.
- Favorites: Word Ladder (127), Course Schedule (207), Alien Dictionary (269), Trapping Rain Water (42), Median of Two Sorted Arrays (4).

### Amazon
- Highest volume of interview questions across all companies.
- Emphasizes **arrays**, **hash tables**, **trees**, and **interval problems**.
- Favorites: Two Sum (1), Merge Intervals (56), LRU Cache (146), Number of Islands (200), Meeting Rooms II (253).

### Meta (Facebook)
- Strongly favors **arrays/strings**, **trees**, and **graphs**.
- Has moved away from DP questions in recent years.
- Favorites: Minimum Remove to Make Valid Parentheses (1249), Binary Tree Vertical Order Traversal (314), Subarray Sum Equals K (560), LRU Cache (146), Random Pick with Weight (528).

### Microsoft
- Balanced across all topics; expects O(n) or O(n log n) solutions.
- Emphasis on **linked lists**, **trees**, **DP**, and **array manipulation**.
- Favorites: Two Sum (1), Reverse Linked List (206), Validate BST (98), LRU Cache (146), Spiral Matrix (54).

### Apple
- SDET interviews focus on **medium-level** LeetCode problems.
- Emphasis on **arrays**, **strings**, **binary search**, and **design patterns**.
- Favorites: Two Sum (1), Valid Parentheses (20), Search in Rotated Sorted Array (33), Merge k Sorted Lists (23), Serialize and Deserialize Binary Tree (297).

### Netflix
- Prioritizes **optimal solutions** (O(n) or O(log n)).
- Focus on **system design** combined with DSA.
- Favorites: Longest Substring Without Repeating Characters (3), LRU Cache (146), Merge Intervals (56), Top K Frequent Elements (347).

### Disney+ Hotstar
- Interview has 5 rounds with 7-8 coding problems across all difficulty levels.
- Focus on **arrays**, **stacks**, **linked lists**, and **DP**.
- Favorites: Stock Span Problem (901), Next Greater Element (496), Detect Cycle in Linked List (141), Maximum Profit from Stock Prices (121).

---

## Recommended Study Order

For maximum interview readiness, follow this order:

### Phase 1: Foundations (Week 1-2)
1. Arrays (Two Sum, Product Except Self, Maximum Subarray)
2. Strings (Valid Anagram, Longest Substring Without Repeating)
3. HashMap/HashSet (Group Anagrams, Top K Frequent)
4. Two Pointers (3Sum, Container With Most Water)
5. Sliding Window (Min Window Substring, Find All Anagrams)

### Phase 2: Core Data Structures (Week 3-4)
6. Linked Lists (Reverse, Merge, Cycle Detection, LRU Cache)
7. Stacks (Valid Parentheses, Daily Temperatures, Basic Calculator)
8. Binary Search (Rotated Array, Search 2D Matrix, Koko Bananas)
9. Trees (Level Order, Validate BST, LCA, Serialize/Deserialize)
10. Heaps (Kth Largest, Merge K Lists, Find Median)

### Phase 3: Advanced Patterns (Week 5-6)
11. Graphs (Number of Islands, Course Schedule, Word Ladder)
12. Dynamic Programming (Coin Change, Word Break, LIS, House Robber)
13. Backtracking (Subsets, Permutations, Combination Sum)
14. Greedy (Jump Game, Meeting Rooms, Partition Labels)

### Phase 4: Polish (Week 7-8)
15. Matrix problems, Sorting, and Recursion
16. Revisit all MUST-DO problems
17. Timed practice (45 min per medium, 60 min per hard)
18. Mock interviews

---

## Key Statistics

- **Total MUST-DO problems across all topics: ~120**
- **Total GOOD-TO-KNOW problems: ~60**
- **87% of FAANG interview questions** are built around 10-12 core patterns
- **Candidates who recognize patterns** have an **85% success rate** vs 35% for those who don't
- **150-200 problems well-understood** is enough for most interviews

---

## Sources

- [NeetCode Blind 75](https://neetcode.io/practice/practice/blind75)
- [Top 100 LeetCode Questions 2025 Edition - Shadecoder](https://www.shadecoder.com/blogs/top-100-leetcode-coding-interview-questions-(2025-edition))
- [LeetCode Patterns for FAANG - Educative](https://www.educative.io/blog/coding-interview-leetcode-patterns)
- [FAANG Coding Interview Questions - GitHub](https://github.com/ombharatiya/FAANG-Coding-Interview-Questions)
- [LeetCode Company-Wise Questions - GitHub](https://github.com/krishnadey30/LeetCode-Questions-CompanyWise)
- [Company-Wise Interview Questions Feb 2026 - GitHub](https://github.com/snehasishroy/leetcode-companywise-interview-questions)
- [Meta's 73 Most Asked LeetCode Problems - Medium](https://medium.com/@johnadjanohoun/metas-most-asked-coding-interview-questions-the-complete-list-of-73-leetcode-problems-47e96767adc7)
- [Google SDE Sheet - GeeksforGeeks](https://www.geeksforgeeks.org/dsa/google-sde-sheet-interview-questions-and-answers/)
- [Amazon Most Frequent DS & Algo Questions 2025 - Medium](https://medium.com/@prashant558908/amazon-most-frequent-ds-algo-questions-in-2025-arranged-by-data-structures-5b876b1d9d05)
- [Top 20 Microsoft DSA Interview Questions - GetSDEReady](https://getsdeready.com/top-20-microsoft-dsa-interview-questions-expert-tips-2025-guide/)
- [Apple SDE Sheet - GeeksforGeeks](https://www.geeksforgeeks.org/dsa/apple-sde-interview-questions-and-answers/)
- [Netflix SDE Sheet - GeeksforGeeks](https://www.geeksforgeeks.org/dsa/netflix-sde-sheet-interview-questions-and-answers/)
- [Disney+ Hotstar Interview Experience - GeeksforGeeks](https://www.geeksforgeeks.org/interview-experiences/disney-hotstar-interview-experience-for-sde-1-2022/)
- [Apple SDET Interview - LeetCode Discussion](https://leetcode.com/discuss/interview-experience/5758686/APPLE-or-SDET-or-HYDERABAD-OFFER/)
- [15 LeetCode Patterns for 90% of FAANG Questions - LockedInAI](https://www.lockedinai.com/blog/master-15-leetcode-patterns)
- [Most Commonly Asked LeetCode Questions - LeetCode Wizard](https://leetcodewizard.io/blog/what-leetcode-questions-are-most-commonly-asked-during-interviews-we-asked-our-users)
- [Advanced Tree Problems for FAANG - GetSDEReady](https://getsdeready.com/advanced-tree-problems-for-faang-interviews/)
- [Top Netflix DSA Interview Questions - GetSDEReady](https://getsdeready.com/top-netflix-dsa-interview-questions-2025-prep-guide/)
- [Amazon Coding Interview Questions - Design Gurus](https://www.designgurus.io/blog/amazon-14-question)
- [Top LeetCode Patterns - Design Gurus](https://www.designgurus.io/blog/top-lc-patterns)
