package ra.hul.dsa.bitmanipulation;

/**
 * Reverse Bits - reverse the 32 bits of an integer via a shift-and-OR loop.
 * LeetCode #190 (Easy)
 *
 * Time: O(1) (32 iterations), Space: O(1)
 */
public class Ques4_ReverseBits {

    public static int reverseBits(int n) {
        int result = 0;
        for (int i = 0; i < 32; i++) {
            result = (result << 1) | (n & 1);  // make room, push n's lowest bit
            n >>>= 1;                          // LOGICAL shift to the next bit
        }
        return result;
    }

    static void main() {
        System.out.println(reverseBits(43261596));   // 964176192
        System.out.println(reverseBits(-3));         // -1073741825
    }
}
