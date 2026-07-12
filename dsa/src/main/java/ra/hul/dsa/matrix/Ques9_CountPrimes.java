package ra.hul.dsa.matrix;

/**
 * Count Primes - count the prime numbers strictly less than n via the Sieve of Eratosthenes.
 *
 * Time: O(n log log n), Space: O(n)
 */
public class Ques9_CountPrimes {

    public static int countPrimes(int n) {
        if (n < 3) return 0;                       // no primes strictly below 2

        boolean[] composite = new boolean[n];      // false = prime (default), index 0..n-1

        // Loop p while p*p < n, using division to avoid int overflow on p*p.
        for (int p = 2; p <= (n - 1) / p; p++) {
            if (!composite[p]) {
                for (long m = (long) p * p; m < n; m += p) {
                    composite[(int) m] = true;     // cross out multiples, starting at p*p
                }
            }
        }

        int count = 0;
        for (int i = 2; i < n; i++) {
            if (!composite[i]) count++;
        }
        return count;
    }

    static void main() {
        System.out.println(countPrimes(10)); // 4
        System.out.println(countPrimes(0));  // 0
        System.out.println(countPrimes(2));  // 0
        System.out.println(countPrimes(3));  // 1
    }
}
