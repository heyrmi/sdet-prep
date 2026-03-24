package ra.hul.dsa.strings;

public class Ques6_ValidAnagram {

    public static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] freqArr = new int[26];

        for (int i = 0; i < s.length(); i++) {
            freqArr[s.charAt(i) - 'a']++;
            freqArr[t.charAt(i) - 'a']--;
        }

        for (int freq : freqArr) {
            if (freq != 0) return false;
        }
        return true;
    }

    static void main() {
        System.out.println(isAnagram("anagram", "nagaram"));  // true
        System.out.println(isAnagram("rat", "car"));          // false
    }
}
