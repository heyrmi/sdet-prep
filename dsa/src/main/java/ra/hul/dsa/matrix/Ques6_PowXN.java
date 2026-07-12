package ra.hul.dsa.matrix;

/**
 * Pow(x, n) - compute x raised to the power n using binary exponentiation.
 *
 * Time: O(log n), Space: O(1)
 */
public class Ques6_PowXN {

    public static double myPow(double x, int n) {
        long e = n;                 // copy to long FIRST: -Integer.MIN_VALUE overflows int
        if (e < 0) {
            x = 1 / x;
            e = -e;
        }
        double result = 1.0;
        while (e > 0) {
            if ((e & 1) == 1) result *= x;  // current bit set -> fold in this power of x
            x *= x;                         // square the base for the next bit
            e >>= 1;                        // drop the lowest bit
        }
        return result;
    }

    static void main() {
        System.out.println(myPow(2.0, 10)); // 1024.0
        System.out.println(myPow(2.1, 3));  // 9.261000000000001
        System.out.println(myPow(2.0, -2)); // 0.25
        System.out.println(myPow(1.0, -7)); // 1.0
    }
}
