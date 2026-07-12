package ra.hul.sdet.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version Comparator - Parse semantic version strings with regex and order them per semver precedence.
 * Common SDET question: "Compare versions like 1.2.3 and 2.0.0-beta.1 to find which is newer".
 *
 * Implements Comparable<Version>. Handles missing patch (1.0 == 1.0.0) and pre-release precedence.
 * Self-contained: main() self-verifies with PASS/FAIL. No network.
 */
public class Ques7_VersionComparator {

    // major.minor(.patch)?(-prerelease)?(+build)? — build metadata is ignored for precedence.
    private static final Pattern SEMVER = Pattern.compile(
            "^(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?(?:-(?<pre>[0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");

    public static final class Version implements Comparable<Version> {
        final int major, minor, patch;
        final String[] pre; // empty array means no pre-release (higher precedence)

        private Version(int major, int minor, int patch, String[] pre) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.pre = pre;
        }

        public static Version parse(String v) {
            Matcher m = SEMVER.matcher(v.trim());
            if (!m.matches()) throw new IllegalArgumentException("Not a semantic version: " + v);
            int major = Integer.parseInt(m.group("major"));
            int minor = Integer.parseInt(m.group("minor"));
            int patch = m.group("patch") == null ? 0 : Integer.parseInt(m.group("patch"));
            String pre = m.group("pre");
            String[] preParts = (pre == null || pre.isEmpty()) ? new String[0] : pre.split("\\.");
            return new Version(major, minor, patch, preParts);
        }

        @Override
        public int compareTo(Version o) {
            int c = Integer.compare(major, o.major);
            if (c != 0) return c;
            c = Integer.compare(minor, o.minor);
            if (c != 0) return c;
            c = Integer.compare(patch, o.patch);
            if (c != 0) return c;
            // A version WITH a pre-release has LOWER precedence than one without.
            if (pre.length == 0 && o.pre.length == 0) return 0;
            if (pre.length == 0) return 1;
            if (o.pre.length == 0) return -1;
            return comparePreRelease(pre, o.pre);
        }

        private static int comparePreRelease(String[] a, String[] b) {
            int n = Math.min(a.length, b.length);
            for (int i = 0; i < n; i++) {
                String x = a[i], y = b[i];
                boolean xn = x.matches("\\d+"), yn = y.matches("\\d+");
                int c;
                if (xn && yn) {
                    c = Integer.compare(Integer.parseInt(x), Integer.parseInt(y));
                } else if (xn) {
                    c = -1; // numeric identifiers rank lower than alphanumeric
                } else if (yn) {
                    c = 1;
                } else {
                    c = x.compareTo(y);
                }
                if (c != 0) return c;
            }
            // All shared identifiers equal: the one with more identifiers is greater.
            return Integer.compare(a.length, b.length);
        }

        @Override
        public String toString() {
            String base = major + "." + minor + "." + patch;
            return pre.length == 0 ? base : base + "-" + String.join(".", pre);
        }
    }

    static void main() {
        record Case(String a, String b, int sign) {}
        Case[] cases = {
                new Case("1.0.0", "1.0.0", 0),
                new Case("1.0", "1.0.0", 0),
                new Case("2.0.0", "1.9.9", 1),
                new Case("1.2.3", "1.2.10", -1),
                new Case("1.0.0-beta.1", "1.0.0-beta.2", -1),
                new Case("1.0.0-alpha", "1.0.0", -1),
                new Case("1.0.0-alpha", "1.0.0-alpha.1", -1),
                new Case("1.0.0-alpha.1", "1.0.0-alpha.beta", -1)
        };

        boolean pass = true;
        for (Case c : cases) {
            int got = Integer.signum(Version.parse(c.a()).compareTo(Version.parse(c.b())));
            boolean ok = got == c.sign();
            System.out.printf("%-16s vs %-18s -> %2d (expected %2d)%n", c.a(), c.b(), got, c.sign());
            pass &= ok;
        }
        System.out.println(pass ? "PASS: version precedence matches semver rules."
                : "FAIL: version comparison mismatch.");
    }
}
