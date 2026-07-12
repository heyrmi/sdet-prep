package ra.hul.dsa.prefixsum;

/**
 * Subarray Sums Divisible by K - count contiguous subarrays whose sum is divisible by k, via remainder counts.
 *
 * Time: O(n), Space: O(k)
 */
public class Ques5_SubarraySumsDivisibleByK {

    public static int subarraysDivByK(int[] nums, int k) {
        int[] seen = new int[k]; // count of each normalized remainder; index 0..k-1
        seen[0] = 1;             // empty prefix has remainder 0
        long cur = 0;
        int count = 0;
        for (int x : nums) {
            cur += x;
            int r = (int) (((cur % k) + k) % k); // normalize: Java's % can be negative
            count += seen[r];                    // earlier prefixes sharing this remainder
            seen[r]++;
        }
        return count;
    }

    static void main() {
        System.out.println(subarraysDivByK(new int[]{4, 5, 0, -2, -3, 1}, 5)); // 7
        System.out.println(subarraysDivByK(new int[]{5}, 9));                  // 0
        System.out.println(subarraysDivByK(new int[]{-1, 2, 9}, 2));           // 2
    }
}
