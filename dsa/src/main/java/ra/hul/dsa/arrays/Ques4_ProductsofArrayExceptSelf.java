package ra.hul.dsa.arrays;

import java.util.Arrays;

public class Ques4_ProductsofArrayExceptSelf {
    public static int[] productExceptSelfDivisionMethod(int[] nums) {
        int product = 1, zeroCount = 0;
        for (int num : nums) {
            if (num == 0)
                zeroCount++;
            else
                product *= num;
        }

        int[] result = new int[nums.length];

        if (zeroCount > 1)
            return result; // since every place is 0

        for (int i = 0; i < nums.length; i++) {
            if (zeroCount > 0)
                result[i] = (nums[i] == 0) ? product : 0;
            else
                result[i] = product / nums[i];

        }

        return result;
    }

    public static int[] productExceptSelfPrefix(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // Left product (forward pass)
        result[0] = 1; // nothing on the left of the first element (prefix)
        for (int i = 1; i < n; i++) {
            result[i] = result[i - 1] * nums[i - 1];
        }

        // Right product (backward pass)
        int right = 1; // nothing on the right of the last element
        for (int i = n - 1; i >= 0; i--) {
            result[i] *= right;
            right *= nums[i];
        }
        return result;
    }

    static void main() {
        // Division method
        System.out.println(Arrays.toString(productExceptSelfDivisionMethod(new int[]{1, 2, 3, 4})));     // [24, 12, 8, 6]
        System.out.println(Arrays.toString(productExceptSelfDivisionMethod(new int[]{-1, 1, 0, -3, 3}))); // [0, 0, 9, 0, 0]
        System.out.println(Arrays.toString(productExceptSelfDivisionMethod(new int[]{0, 0})));            // [0, 0]

        // Prefix method
        System.out.println(Arrays.toString(productExceptSelfPrefix(new int[]{1, 2, 3, 4})));              // [24, 12, 8, 6]
        System.out.println(Arrays.toString(productExceptSelfPrefix(new int[]{-1, 1, 0, -3, 3})));        // [0, 0, 9, 0, 0]
        System.out.println(Arrays.toString(productExceptSelfPrefix(new int[]{0, 0})));                   // [0, 0]
    }
}