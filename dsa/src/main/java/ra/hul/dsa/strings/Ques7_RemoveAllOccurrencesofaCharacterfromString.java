package ra.hul.dsa.strings;

public class Ques7_RemoveAllOccurrencesofaCharacterfromString {

    public static String removeChar(String str, char target) {
        if (str == null || str.isEmpty()) return str;

        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray()) {
            if(ch != target)
                sb.append(ch);
        }
        return sb.toString();
    }

    static void main() {
        System.out.println(removeChar("hello world", 'o'));  // "hell wrld"
        System.out.println(removeChar("aaaa", 'a'));          // ""
        System.out.println(removeChar("abc", 'z'));           // "abc"
    }
}
