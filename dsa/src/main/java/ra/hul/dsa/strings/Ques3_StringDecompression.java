package ra.hul.dsa.strings;

public class Ques3_StringDecompression {
    public static String decompress(String str) {
        if (str == null || str.isEmpty()) return str;

        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (i < str.length()) {
            int count = 0;

            while (i < str.length() && Character.isDigit(str.charAt(i))) {
                count = count * 10 + (str.charAt(i) - '0'); // handling multiple digits
                i++;
            }

            // Append character n times
            if (i < str.length()) {
                sb.append(String.valueOf(str.charAt(i)).repeat(count));
                i++;
            }
        }
        return sb.toString();
    }


    static void main() {
        System.out.println(decompress("3A2B2C"));   // "AAABBCC"
        System.out.println(decompress("1A1B1C"));   // "ABC"
        System.out.println(decompress("12A"));       // "AAAAAAAAAAAA" (multi-digit)
        System.out.println(decompress(""));          // ""
    }
}
