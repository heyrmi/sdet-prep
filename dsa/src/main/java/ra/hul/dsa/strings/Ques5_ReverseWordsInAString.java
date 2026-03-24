package ra.hul.dsa.strings;

public class Ques5_ReverseWordsInAString {

    public static String reverseWords(String s) {
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0)
                sb.append(" ");
        }

        return sb.toString();
    }

    static void main() {
        System.out.println(reverseWords("  hello  world  "));   // "world hello"
        System.out.println(reverseWords("the sky is blue"));    // "blue is sky the"
    }
}
