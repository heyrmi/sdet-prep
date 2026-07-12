package ra.hul.dsa.slidingwindow;

/**
 * Max Consecutive Ones III - longest run of 1s after flipping at most k zeros (longest window with <= k zeros).
 *
 * Time: O(n), Space: O(1)
 */
public class Ques7_MaxConsecutiveOnesIii {

    public static int longestOnes(int[] nums, int k) {
        int left = 0, zeros = 0, best = 0;
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) zeros++;        // a zero entered the window
            while (zeros > k) {                    // too many zeros -> shrink from the left
                if (nums[left] == 0) zeros--;
                left++;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static void main() {
        System.out.println(longestOnes(new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0}, 2));                          // 6
        System.out.println(longestOnes(new int[]{0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1}, 3)); // 10
        System.out.println(longestOnes(new int[]{0, 0, 0}, 0));                                                  // 0
    }
}
