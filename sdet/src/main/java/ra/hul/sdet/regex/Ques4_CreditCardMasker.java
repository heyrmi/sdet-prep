package ra.hul.sdet.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Credit Card Masker - Find card numbers in free text and mask all but the last 4 digits.
 * Common SDET question: "Redact PAN numbers (16-digit and 15-digit AMEX), preserving the original format".
 *
 * Self-contained: main() self-verifies with PASS/FAIL. No network.
 */
public class Ques4_CreditCardMasker {

    // Candidate card: 13-16 digits, optionally grouped by single spaces or dashes.
    // \b anchors avoid swallowing longer digit runs (e.g. order ids).
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){12,15}\\d\\b");

    public static String mask(String text) {
        Matcher m = CARD.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(maskNumber(m.group())));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Replaces every digit except the final 4 with '*', keeping separators intact. */
    private static String maskNumber(String candidate) {
        int totalDigits = 0;
        for (int i = 0; i < candidate.length(); i++) {
            if (Character.isDigit(candidate.charAt(i))) totalDigits++;
        }
        StringBuilder sb = new StringBuilder();
        int seen = 0;
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(seen < totalDigits - 4 ? '*' : c);
                seen++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static void main() {
        String[] inputs = {
                "Paid with 4111-1111-1111-1111 today",
                "Card 4111 1111 1111 1111 on file",
                "Raw 4111111111111111 stored",
                "AMEX 3782 822463 10005 charged"
        };
        String[] expected = {
                "Paid with ****-****-****-1111 today",
                "Card **** **** **** 1111 on file",
                "Raw ************1111 stored",
                "AMEX **** ****** *0005 charged"
        };

        boolean pass = true;
        for (int i = 0; i < inputs.length; i++) {
            String got = mask(inputs[i]);
            boolean ok = got.equals(expected[i]);
            System.out.printf("%-40s -> %s%n", inputs[i], got);
            pass &= ok;
        }
        System.out.println(pass ? "PASS: all card numbers masked with format preserved."
                : "FAIL: masking mismatch.");
    }
}
