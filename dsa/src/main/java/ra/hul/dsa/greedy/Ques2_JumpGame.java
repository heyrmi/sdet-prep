package ra.hul.dsa.greedy;

/**
 * Jump Game - can you reach the last index, where each value is the max jump length from that index.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques2_JumpGame {

    public static boolean canJump(int[] nums) {
        int reach = 0; // furthest index reachable so far
        for (int i = 0; i < nums.length; i++) {
            if (i > reach) return false;           // stranded before this index
            reach = Math.max(reach, i + nums[i]);  // greedily extend the frontier
        }
        return true; // finished the scan => last index was reachable
    }

    static void main() {
        System.out.println(canJump(new int[]{2, 3, 1, 1, 4})); // true
        System.out.println(canJump(new int[]{3, 2, 1, 0, 4})); // false
        System.out.println(canJump(new int[]{0}));             // true
    }
}
