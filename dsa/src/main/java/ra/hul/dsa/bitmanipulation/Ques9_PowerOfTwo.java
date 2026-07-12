package ra.hul.dsa.bitmanipulation;

/**
 * Power of Two - true iff n is positive and has exactly one set bit via n & (n-1).
 * LeetCode #231 (Easy)
 *
 * Time: O(1), Space: O(1)
 */
public class Ques9_PowerOfTwo {

    public static boolean isPowerOfTwo(int n) {
        // One set bit, and positive. The guard rejects 0 and Integer.MIN_VALUE.
        return n > 0 && (n & (n - 1)) == 0;
    }

    static void main() {
        System.out.println(isPowerOfTwo(1));     // true
        System.out.println(isPowerOfTwo(16));    // true
        System.out.println(isPowerOfTwo(3));     // false
        System.out.println(isPowerOfTwo(0));     // false
        System.out.println(isPowerOfTwo(-16));   // false
    }
}
