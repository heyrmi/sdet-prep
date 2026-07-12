package ra.hul.dsa.dp;

/**
 * Partition Equal Subset Sum - Can nums be split into two subsets with equal sum?
 *
 * Time: O(n * target), Space: O(target) - subset-sum to total/2; 1D 0/1 knapsack, inner loop backwards.
 */
public class Ques9_PartitionEqualSubsetSum {

    public static boolean canPartition(int[] nums) {
        int total = 0;
        for (int x : nums) total += x;
        if ((total & 1) == 1) return false;       // odd total can't be halved
        int target = total / 2;

        boolean[] dp = new boolean[target + 1];   // dp[a] = subset summing to a is reachable
        dp[0] = true;                             // empty subset sums to 0
        for (int x : nums) {
            for (int a = target; a >= x; a--) {   // BACKWARDS: use each x at most once
                if (dp[a - x]) dp[a] = true;
            }
            if (dp[target]) return true;          // optional early exit
        }
        return dp[target];
    }

    static void main() {
        System.out.println(canPartition(new int[]{1, 5, 11, 5})); // true
        System.out.println(canPartition(new int[]{1, 2, 3, 5}));  // false
        System.out.println(canPartition(new int[]{1, 1}));        // true
    }
}
