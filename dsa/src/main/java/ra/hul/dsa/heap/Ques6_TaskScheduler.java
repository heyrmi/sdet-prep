package ra.hul.dsa.heap;

/**
 * Task Scheduler - minimum time to run all tasks with an n-unit cooldown between identical tasks.
 * LeetCode #621 (Medium)
 *
 * Time: O(total + 26), Space: O(1)
 */
public class Ques6_TaskScheduler {

    public static int leastInterval(char[] tasks, int n) {
        int[] counts = new int[26];
        for (char c : tasks) counts[c - 'A']++;

        int maxCount = 0;
        for (int c : counts) maxCount = Math.max(maxCount, c);

        int maxItems = 0; // how many tasks share the top frequency
        for (int c : counts) if (c == maxCount) maxItems++;

        int frames = (maxCount - 1) * (n + 1) + maxItems;
        return Math.max(frames, tasks.length);
    }

    static void main() {
        System.out.println(leastInterval(new char[]{'A', 'A', 'A', 'B', 'B', 'B'}, 2)); // 8
        System.out.println(leastInterval(new char[]{'A', 'A', 'A', 'B', 'B', 'B'}, 0)); // 6
        System.out.println(leastInterval(
                new char[]{'A', 'A', 'A', 'A', 'A', 'A', 'B', 'C', 'D', 'E', 'F', 'G'}, 2)); // 16
    }
}
