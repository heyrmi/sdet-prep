package ra.hul.tests.data;

import io.qameta.allure.*;
import net.datafaker.Faker;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.api.models.PostPayload;
import ra.hul.framework.api.models.User;
import ra.hul.framework.data.CredentialFactory;
import ra.hul.framework.data.Credentials;
import ra.hul.framework.data.FakerProvider;
import ra.hul.framework.data.PostPayloadFactory;
import ra.hul.framework.data.UserFactory;

/**
 * Verifies the datafaker-backed test-data factories: deterministic reproducibility under a fixed
 * seed, independence across seeds, and that per-field overrides win over generated values.
 * No browser or network required.
 */
@Epic("Test Data Management")
@Feature("Datafaker Factories")
public class DataFactoryTest {

    @Test(groups = {"regression"},
          description = "Same seed produces identical User data (reproducible)")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Seeded reproducibility")
    public void userFactory_sameSeed_shouldProduceIdenticalData() {
        User first = UserFactory.newUser(FakerProvider.seeded(42)).build();
        User second = UserFactory.newUser(FakerProvider.seeded(42)).build();

        Assert.assertEquals(second, first, "Same seed must yield identical User objects");
    }

    @Test(groups = {"regression"},
          description = "Config-seeded factory is reproducible across calls")
    @Severity(SeverityLevel.NORMAL)
    @Story("Seeded reproducibility")
    public void userFactory_configSeed_shouldBeReproducible() {
        User first = UserFactory.newUser().build();
        User second = UserFactory.newUser().build();

        Assert.assertEquals(second, first, "Config-seeded factory must be reproducible");
        Assert.assertNotNull(first.getName());
        Assert.assertTrue(first.getEmail().contains("@"), "Generated email should look like an email");
    }

    @Test(groups = {"regression"},
          description = "Different seeds produce different User data")
    @Severity(SeverityLevel.NORMAL)
    @Story("Seed independence")
    public void userFactory_differentSeeds_shouldProduceDifferentData() {
        User a = UserFactory.newUser(FakerProvider.seeded(1)).build();
        User b = UserFactory.newUser(FakerProvider.seeded(999)).build();

        Assert.assertNotEquals(b, a, "Different seeds should yield different User objects");
    }

    @Test(groups = {"regression"},
          description = "Field overrides win over generated values")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Overrides")
    public void userFactory_overrides_shouldWin() {
        User user = UserFactory.newUser(FakerProvider.seeded(42))
                .withId(7)
                .withName("Rahul Mishra")
                .withEmail("rahul@example.com")
                .withJob("Principal SDET")
                .build();

        Assert.assertEquals(user.getId(), 7);
        Assert.assertEquals(user.getName(), "Rahul Mishra");
        Assert.assertEquals(user.getEmail(), "rahul@example.com");
        Assert.assertEquals(user.getJob(), "Principal SDET");
    }

    @Test(groups = {"regression"},
          description = "PostPayload factory is seed-reproducible and honours overrides")
    @Severity(SeverityLevel.NORMAL)
    @Story("PostPayload factory")
    public void postPayloadFactory_seedAndOverride_shouldBehave() {
        PostPayload first = PostPayloadFactory.newPost(FakerProvider.seeded(7)).build();
        PostPayload second = PostPayloadFactory.newPost(FakerProvider.seeded(7)).build();
        Assert.assertEquals(second, first, "Same seed must yield identical PostPayload");

        PostPayload overridden = PostPayloadFactory.newPost(FakerProvider.seeded(7))
                .withUserId(123)
                .withTitle("Fixed Title")
                .build();
        Assert.assertEquals(overridden.getUserId(), 123);
        Assert.assertEquals(overridden.getTitle(), "Fixed Title");
        Assert.assertNotNull(overridden.getBody(), "Non-overridden body should still be generated");
    }

    @Test(groups = {"regression"},
          description = "Credential factory is seed-reproducible and honours overrides")
    @Severity(SeverityLevel.NORMAL)
    @Story("Credential factory")
    public void credentialFactory_seedAndOverride_shouldBehave() {
        Faker faker = FakerProvider.seeded(100);
        Credentials generated = CredentialFactory.newCredentials(FakerProvider.seeded(100)).build();
        Credentials again = CredentialFactory.newCredentials(FakerProvider.seeded(100)).build();
        Assert.assertEquals(again, generated, "Same seed must yield identical Credentials");
        Assert.assertNotNull(generated.getUsername());

        Credentials custom = CredentialFactory.newCredentials(faker)
                .withUsername("qa_bot")
                .withPassword("Sup3rSecret!")
                .build();
        Assert.assertEquals(custom.getUsername(), "qa_bot");
        Assert.assertEquals(custom.getPassword(), "Sup3rSecret!");
        Assert.assertNotNull(custom.getEmail(), "Non-overridden email should still be generated");
    }
}
