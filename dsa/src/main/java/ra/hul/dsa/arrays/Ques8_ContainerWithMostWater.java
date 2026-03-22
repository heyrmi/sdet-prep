package ra.hul.dsa.arrays;

public class Ques8_ContainerWithMostWater {
    public static int maxArea(int[] heights) {
        int left = 0, right = heights.length - 1;
        int maxArea = Integer.MIN_VALUE;

        while (left < right) {
            // area = width (diff of indexes) * min (left, right) heights
            int width = right - left;
            int minHeight = Math.min(heights[left], heights[right]);

            int area = width * minHeight;

            if (heights[left] < heights[right]) {
                left++;
            } else {
                right--;
            }

            maxArea = Math.max(maxArea, area);
        }
        return maxArea;
    }

    static void main() {
        System.out.println(maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7}));    // 49
        System.out.println(maxArea(new int[]{1, 1}));                         // 1
        System.out.println(maxArea(new int[]{4, 3, 2, 1, 4}));                // 16
        System.out.println(maxArea(new int[]{1, 2, 1}));                      // 2
    }
}
