package ra.hul.dsa.fastslowpointers;

/**
 * Happy Number - Return true if repeatedly summing squares of digits reaches 1, using Floyd's on the implicit sequence.
 *
 * Time: O(log n), Space: O(1)
 */
public class Ques2_HappyNumber {

    public static boolean isHappy(int n) {
        int slow = n, fast = n;
        do {
            slow = next(slow);        // +1
            fast = next(next(fast));  // +2
        } while (slow != fast);
        return slow == 1;             // met at 1 -> happy; met elsewhere -> in a cycle
    }

    private static int next(int x) {
        int sum = 0;
        while (x > 0) {
            int d = x % 10;
            sum += d * d;
            x /= 10;
        }
        return sum;
    }

    static void main() {
        System.out.println(isHappy(19));  // true
        System.out.println(isHappy(2));   // false
        System.out.println(isHappy(1));   // true
        System.out.println(isHappy(7));   // true
    }
}
