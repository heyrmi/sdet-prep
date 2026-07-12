package ra.hul.sdet.designpatterns;

/**
 * Builder Pattern for Test Data - fluent construction of an immutable object with defaults + overrides.
 * Common SDET question: "Build complex test data (User/Order) with defaults, overrides, and validation."
 *
 * Self-contained. In real projects the defaults could come from a Faker library for random data.
 * Run main() — no browser/network needed.
 */
public class Ques4_BuilderTestData {

    /** Immutable test-data object; only the Builder can set fields. */
    static final class User {
        private final String username;
        private final String email;
        private final int age;
        private final String role;
        private final boolean active;

        private User(Builder b) {
            this.username = b.username;
            this.email = b.email;
            this.age = b.age;
            this.role = b.role;
            this.active = b.active;
        }

        String username() { return username; }
        String email() { return email; }
        String role() { return role; }
        boolean active() { return active; }

        @Override public String toString() {
            return "User{username=%s, email=%s, age=%d, role=%s, active=%s}"
                    .formatted(username, email, age, role, active);
        }

        static Builder builder(String username) { return new Builder(username); }

        static final class Builder {
            private final String username;              // required
            private String email;                       // defaulted below
            private int age = 30;                       // default
            private String role = "USER";               // default
            private boolean active = true;              // default

            Builder(String username) { this.username = username; }

            Builder email(String v) { this.email = v; return this; }
            Builder age(int v) { this.age = v; return this; }
            Builder role(String v) { this.role = v; return this; }
            Builder active(boolean v) { this.active = v; return this; }

            User build() {
                if (username == null || username.isBlank())
                    throw new IllegalStateException("username is required");
                if (email == null) email = username + "@test.example";  // derived default
                if (age < 0 || age > 150) throw new IllegalStateException("age out of range: " + age);
                return new User(this);
            }
        }
    }

    static void main() {
        // Minimal: only the required field, everything else defaulted.
        User def = User.builder("alice").build();
        // Full override.
        User admin = User.builder("bob").email("bob@corp.example").age(45).role("ADMIN").active(false).build();

        System.out.println("Defaults : " + def);
        System.out.println("Overrides: " + admin);

        boolean validationOk;
        try { User.builder(" ").build(); validationOk = false; }
        catch (IllegalStateException e) { validationOk = true; }

        boolean ok = def.email().equals("alice@test.example") && def.role().equals("USER") && def.active()
                && admin.role().equals("ADMIN") && !admin.active() && validationOk;
        System.out.println(ok ? "PASSED: builder applied defaults, overrides, and validation."
                              : "FAILED: builder behavior unexpected.");
    }
}
