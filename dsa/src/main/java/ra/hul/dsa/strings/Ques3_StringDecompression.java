package ra.hul.dsa.strings;

public class Ques3_StringDecompression {
    public static String decompress(String str) {
        if (str == null || str.isEmpty()) return str;

        StringBuilder sb = new StringBuilder();
        int read = 0;

        while (read < str.length()) {
            int count = 0;

            while (read < str.length() && Character.isDigit(str.charAt(read))) {
                count = count * 10 + (str.charAt(read) - '0'); // handling multiple digits
                read++;
            }

            // Append character n times
            if (read < str.length()) {
                sb.append(String.valueOf(str.charAt(read)).repeat(count));
                read++;
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
