package ra.hul.framework.data;

import net.datafaker.Faker;

/**
 * Fluent factory that builds {@link Credentials} test data backed by a seeded {@link Faker}.
 * Deterministic under a fixed {@code data.faker.seed}; overrides win over generated values.
 *
 * <p>NOTE: these are synthetic credentials for API/data-driven tests. Do not use them for the
 * live web login demo (that requires the real {@code tomsmith}/{@code SuperSecretPassword!}).</p>
 *
 * <pre>{@code
 * Credentials c = CredentialFactory.newCredentials().withUsername("qa_bot").build();
 * }</pre>
 */
public final class CredentialFactory {

    private final Faker faker;

    private String username;
    private String password;
    private String email;

    private CredentialFactory(Faker faker) {
        this.faker = faker;
    }

    /** Factory seeded from config ({@code data.faker.seed} / {@code data.faker.locale}). */
    public static CredentialFactory newCredentials() {
        return new CredentialFactory(FakerProvider.seeded());
    }

    /** Factory backed by a caller-supplied Faker (e.g. a specific seed). */
    public static CredentialFactory newCredentials(Faker faker) {
        return new CredentialFactory(faker);
    }

    public CredentialFactory withUsername(String username) {
        this.username = username;
        return this;
    }

    public CredentialFactory withPassword(String password) {
        this.password = password;
        return this;
    }

    public CredentialFactory withEmail(String email) {
        this.email = email;
        return this;
    }

    public Credentials build() {
        String genUsername = faker.internet().username();
        String genPassword = faker.internet().password(10, 16, true);
        String genEmail = faker.internet().emailAddress();

        return Credentials.builder()
                .username(username != null ? username : genUsername)
                .password(password != null ? password : genPassword)
                .email(email != null ? email : genEmail)
                .build();
    }
}
