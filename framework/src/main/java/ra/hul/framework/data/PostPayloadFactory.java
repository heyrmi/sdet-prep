package ra.hul.framework.data;

import net.datafaker.Faker;
import ra.hul.framework.api.models.PostPayload;

/**
 * Fluent factory that builds {@link PostPayload} test data backed by a seeded {@link Faker}.
 * Deterministic under a fixed {@code data.faker.seed}; overrides win over generated values.
 *
 * <pre>{@code
 * PostPayload p = PostPayloadFactory.newPost().withUserId(7).build();
 * }</pre>
 */
public final class PostPayloadFactory {

    private final Faker faker;

    private String title;
    private String body;
    private Integer userId;

    private PostPayloadFactory(Faker faker) {
        this.faker = faker;
    }

    /** Factory seeded from config ({@code data.faker.seed} / {@code data.faker.locale}). */
    public static PostPayloadFactory newPost() {
        return new PostPayloadFactory(FakerProvider.seeded());
    }

    /** Factory backed by a caller-supplied Faker (e.g. a specific seed). */
    public static PostPayloadFactory newPost(Faker faker) {
        return new PostPayloadFactory(faker);
    }

    public PostPayloadFactory withTitle(String title) {
        this.title = title;
        return this;
    }

    public PostPayloadFactory withBody(String body) {
        this.body = body;
        return this;
    }

    public PostPayloadFactory withUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public PostPayload build() {
        String genTitle = faker.lorem().sentence(4);
        String genBody = faker.lorem().paragraph(2);
        int genUserId = faker.number().numberBetween(1, 1_000);

        return PostPayload.builder()
                .title(title != null ? title : genTitle)
                .body(body != null ? body : genBody)
                .userId(userId != null ? userId : genUserId)
                .build();
    }
}
