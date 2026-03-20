package ra.hul.dsa.strings;

/**
 * Reverse String - Reverse a character array in-place.
 * LeetCode #344 (Easy)
 *
 * Time: O(n), Space: O(1) - two pointer swap
 */
public class Ques1_ReverseString {

    public static void reverseString(char[] s) {
        int left = 0, right = s.length - 1;
        while (left < right) {
            char temp = s[left];
            s[left] = s[right];
            s[right] = temp;
            left++;
            right--;
        }
    }

    public static void main(String[] args) {
        char[] s1 = {'h', 'e', 'l', 'l', 'o'};
        reverseString(s1);
        System.out.println(s1); // olleh

        char[] s2 = {'H', 'a', 'n', 'n', 'a', 'h'};
        reverseString(s2);
        System.out.println(s2); // hannaH
    }
}
