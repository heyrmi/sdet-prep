package ra.hul.dsa.twopointers;

/**
 * Trapping Rain Water - total water trapped between bars, using two pointers with running maxes.
 *
 * Time: O(n), Space: O(1)
 */
public class Ques3_TrappingRainWater {

    public static int trap(int[] height) {
        int left = 0, right = height.length - 1;
        int leftMax = 0, rightMax = 0, water = 0;
        while (left < right) {
            if (height[left] < height[right]) {
                // left side is the binding wall; rightMax is guaranteed >= leftMax here
                leftMax = Math.max(leftMax, height[left]);
                water += leftMax - height[left];
                left++;
            } else {
                rightMax = Math.max(rightMax, height[right]);
                water += rightMax - height[right];
                right--;
            }
        }
        return water;
    }

    static void main() {
        System.out.println(trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1})); // 6
        System.out.println(trap(new int[]{4, 2, 0, 3, 2, 5}));                   // 9
        System.out.println(trap(new int[]{3, 0, 2, 0, 4}));                      // 7
        System.out.println(trap(new int[]{1, 2, 3, 4, 5}));                      // 0
    }
}
