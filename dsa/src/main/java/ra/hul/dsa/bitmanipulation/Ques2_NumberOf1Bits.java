package ra.hul.dsa.bitmanipulation;

/**
 * Number of 1 Bits - count set bits (Hamming weight) using Kernighan's x & (x-1) trick.
 * LeetCode #191 (Easy)
 *
 * Time: O(#set bits), Space: O(1)
 */
public class Ques2_NumberOf1Bits {

    public static int hammingWeight(int n) {
        int count = 0;
        while (n != 0) {
            n &= (n - 1);   // clear the lowest set bit
            count++;
        }
        return count;
    }

    static void main() {
        System.out.println(hammingWeight(11));   // 3
        System.out.println(hammingWeight(128));  // 1
        System.out.println(hammingWeight(-1));   // 32
        System.out.println(hammingWeight(0));    // 0
    }
}
