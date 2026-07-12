package ra.hul.framework.data;

import net.datafaker.Faker;
import ra.hul.framework.api.models.User;

/**
 * Fluent factory that builds {@link User} test data backed by a seeded {@link Faker}.
 *
 * <p>Deterministic: two factories created via {@link #newUser()} with the same configured
 * {@code data.faker.seed}/{@code data.faker.locale} produce identical users. To keep the
 * faker consumption order stable regardless of which fields are overridden, {@link #build()}
 * always generates every field in a fixed order and then applies any overrides on top.</p>
 *
 * <pre>{@code
 * User u = UserFactory.newUser().withName("Rahul").build();
 * }</pre>
 */
public final class UserFactory {

    private final Faker faker;

    private Integer id;
    private String name;
    private String email;
    private String job;

    private UserFactory(Faker faker) {
        this.faker = faker;
    }

    /** Factory seeded from config ({@code data.faker.seed} / {@code data.faker.locale}). */
    public static UserFactory newUser() {
        return new UserFactory(FakerProvider.seeded());
    }

    /** Factory backed by a caller-supplied Faker (e.g. a specific seed). */
    public static UserFactory newUser(Faker faker) {
        return new UserFactory(faker);
    }

    public UserFactory withId(int id) {
        this.id = id;
        return this;
    }

    public UserFactory withName(String name) {
        this.name = name;
        return this;
    }

    public UserFactory withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserFactory withJob(String job) {
        this.job = job;
        return this;
    }

    public User build() {
        // Generate all fields in a fixed order so faker consumption is deterministic,
        // then let explicit overrides win.
        int genId = faker.number().numberBetween(1, 100_000);
        String genName = faker.name().fullName();
        String genEmail = faker.internet().emailAddress();
        String genJob = faker.job().position();

        return User.builder()
                .id(id != null ? id : genId)
                .name(name != null ? name : genName)
                .email(email != null ? email : genEmail)
                .job(job != null ? job : genJob)
                .build();
    }
}
