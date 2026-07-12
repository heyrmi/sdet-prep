package ra.hul.sdet.regex;

import java.util.regex.Pattern;

/**
 * Email Validator - Regex-based validation of email addresses.
 * Common SDET question: "Validate emails with regex — accept subdomains and plus-addressing, reject malformed ones".
 *
 * Self-contained: main() self-verifies with PASS/FAIL. No network.
 */
public class Ques1_EmailValidator {

    // Local part: alphanumeric groups separated by single . _ + or -.
    // Domain: one or more labels ending in a TLD of >= 2 letters.
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9]+([._+-][A-Za-z0-9]+)*@([A-Za-z0-9]+([-][A-Za-z0-9]+)*\\.)+[A-Za-z]{2,}$");

    public static boolean isValid(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    static void main() {
        String[] valid = {
                "user@domain.com",
                "user.name+tag@sub.domain.co.in",
                "a_b-c@my-domain.org",
                "John.Doe@example.COM"
        };
        String[] invalid = {
                "@domain.com",
                "user@",
                "user @domain.com",
                "user@.com",
                "user@domain",
                "user..name@domain.com",
                "user@domain..com"
        };

        boolean allPass = true;
        for (String e : valid) {
            boolean ok = isValid(e);
            System.out.printf("%-35s -> %-7s (expected VALID)%n", e, ok ? "VALID" : "INVALID");
            allPass &= ok;
        }
        for (String e : invalid) {
            boolean ok = isValid(e);
            System.out.printf("%-35s -> %-7s (expected INVALID)%n", e, ok ? "VALID" : "INVALID");
            allPass &= !ok;
        }

        System.out.println(allPass ? "PASS: all email cases classified correctly." : "FAIL: at least one email misclassified.");
    }
}
