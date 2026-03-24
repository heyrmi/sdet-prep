package ra.hul.dsa.strings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Asked In Apple SDET rounds
 */
public class Ques2_StringCompressionI {

    public static String compress(String str) {
        if (str == null || str.isEmpty()) return str;

        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 1; i <= str.length(); i++) {
            if (i < str.length() && str.charAt(i) == str.charAt(i - 1)) {
                count++;
            } else {
                sb.append(count).append(str.charAt(i - 1));
                count = 1;
            }
        }
        return sb.toString();
    }

    // Merge Same characters globally
    public static String compressMerged(String str) {
        if (str == null || str.isEmpty()) return str;

        Map<Character, Integer> freqMap = new LinkedHashMap<>();
        for (char ch : str.toCharArray()) {
            freqMap.put(ch, freqMap.getOrDefault(ch, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        freqMap.forEach((ch, count) -> sb.append(count).append(ch));
        return sb.toString();
    }

    static void main() {
        System.out.println(compress("AAABBCC"));      // "3A2B2C"
        System.out.println(compress("A"));             // "1A"
        System.out.println(compress(""));              // ""
        System.out.println(compress("ABCDEF"));        // "1A1B1C1D1E1F"
        System.out.println(compress("AAAAAA"));        // "6A"
        System.out.println(compress(null));             // null


        System.out.println(compressMerged("AAABBAA")); //5A2B
    }
}
