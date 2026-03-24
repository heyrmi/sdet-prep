package ra.hul.dsa.strings;

public class Ques3_StringCompressionII {

    public static int compress(char[] chars) {
        int read = 0;
        int write = 0;

        while (read < chars.length) {
            char currentChar = chars[read];
            int count = 0;

            while (read < chars.length && chars[read] == currentChar) {
                read++;
                count++;
            }

            // write character count
            chars[write++] = currentChar;
            if (count > 1) {
                String countStr = String.valueOf(count);
                for (char ch : countStr.toCharArray()) {
                    chars[write++] = ch;
                }
            }
        }
        return write;
    }

    static void main() {
        char[] c1 = {'A','A','A','B','B','C','C'};
        int len1 = compress(c1);
        System.out.println(new String(c1, 0, len1));  // "A3B2C2"

        char[] c2 = {'A'};
        int len2 = compress(c2);
        System.out.println(new String(c2, 0, len2));  // "A"

        char[] c3 = {'A','B','C','D','E','F'};
        int len3 = compress(c3);
        System.out.println(new String(c3, 0, len3));  // "ABCDEF"

        char[] c4 = {'A','A','A','A','A','A'};
        int len4 = compress(c4);
        System.out.println(new String(c4, 0, len4));  // "A6"

        char[] c5 = {'a','a','b','b','c','c','c'};
        int len5 = compress(c5);
        System.out.println(new String(c5, 0, len5));  // "a2b2c3"

        char[] c6 = {'a','b','b','b','b','b','b','b','b','b','b','b','b'};
        int len6 = compress(c6);
        System.out.println(new String(c6, 0, len6));  // "ab12"
    }
}
