package ra.hul.dsa.matrix;

/**
 * Excel Sheet Column Number - convert an Excel column title (bijective base-26) to its number.
 *
 * Time: O(L), Space: O(1)
 */
public class Ques8_ExcelSheetColumnNumber {

    public static int titleToNumber(String columnTitle) {
        int result = 0;
        for (int i = 0; i < columnTitle.length(); i++) {
            int digit = columnTitle.charAt(i) - 'A' + 1;  // A=1 .. Z=26 (no zero digit)
            result = result * 26 + digit;
        }
        return result;
    }

    static void main() {
        System.out.println(titleToNumber("A"));  // 1
        System.out.println(titleToNumber("AB")); // 28
        System.out.println(titleToNumber("ZY")); // 701
    }
}
