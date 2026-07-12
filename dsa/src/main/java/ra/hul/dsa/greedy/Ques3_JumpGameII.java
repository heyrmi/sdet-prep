package ra.hul.dsa.greedy;

/**
 * Jump Game II - minimum number of jumps to reach the last index.
 *
 * Time: O(n), Space: O(1) - greedy level-by-level (implicit BFS)
 */
public class Ques3_JumpGameII {

    public static int jump(int[] nums) {
        int jumps = 0;
        int currentEnd = 0;   // furthest index reachable with `jumps` jumps
        int farthest = 0;     // furthest index reachable with one more jump
        // Stop at n-2: standing on the last index never requires another jump.
        for (int i = 0; i < nums.length - 1; i++) {
            farthest = Math.max(farthest, i + nums[i]);
            if (i == currentEnd) {     // exhausted current level -> must jump
                jumps++;
                currentEnd = farthest;
            }
        }
        return jumps;
    }

    static void main() {
        System.out.println(jump(new int[]{2, 3, 1, 1, 4})); // 2
        System.out.println(jump(new int[]{2, 3, 0, 1, 4})); // 2
        System.out.println(jump(new int[]{0}));             // 0
    }
}
