package ra.hul.framework.data;

import net.datafaker.Faker;
import ra.hul.framework.core.config.ConfigManager;

import java.util.Locale;
import java.util.Random;

/**
 * Central factory for {@link Faker} instances.
 *
 * <p>Generation is made <b>deterministic</b> by seeding datafaker's random source from
 * config: {@code data.faker.seed} (long) and {@code data.faker.locale} (BCP-47 language tag).
 * Two Fakers built from the same seed/locale emit the identical sequence of values, which is
 * what makes the factory tests reproducible.</p>
 *
 * <p>Both keys are read via {@code getOrDefault}/{@code getLongOrDefault} so their absence
 * never crashes — defaults are seed {@code 1337} and locale {@code en}.</p>
 */
public final class FakerProvider {

    public static final long DEFAULT_SEED = 1337L;
    public static final String DEFAULT_LOCALE = "en";

    private FakerProvider() {
    }

    /** Faker seeded from the configured {@code data.faker.seed} / {@code data.faker.locale}. */
    public static Faker seeded() {
        long seed = ConfigManager.getLongOrDefault("data.faker.seed", DEFAULT_SEED);
        String locale = ConfigManager.getOrDefault("data.faker.locale", DEFAULT_LOCALE);
        return seeded(seed, locale);
    }

    /** Faker seeded from an explicit seed, using the configured locale. */
    public static Faker seeded(long seed) {
        String locale = ConfigManager.getOrDefault("data.faker.locale", DEFAULT_LOCALE);
        return seeded(seed, locale);
    }

    /** Faker seeded from an explicit seed and locale. */
    public static Faker seeded(long seed, String locale) {
        return new Faker(Locale.forLanguageTag(locale), new Random(seed));
    }
}
