package ra.hul.tests.contract;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.api.client.ApiClient;
import ra.hul.framework.api.models.User;
import ra.hul.framework.core.config.ConfigManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;

/**
 * Real Pact JVM <b>consumer</b> contract test driven programmatically (no JUnit5 extension — this
 * project runs on TestNG). Builds a V4 pact for consumer {@code FrameworkClient} ↔ provider
 * {@code UserService}, spins up the Pact mock server, points the framework's {@link ApiClient} at
 * it, verifies the response deserializes to the {@link User} POJO, and confirms the pact file is
 * written to the configured output dir ({@code pact.output.dir}, default {@code target/pacts}).
 */
@Epic("API Automation")
@Feature("Contract Testing (Pact JVM)")
public class ConsumerContractTest {

    private static final Logger log = LogManager.getLogger(ConsumerContractTest.class);

    private static final String CONSUMER = "FrameworkClient";
    private static final String PROVIDER = "UserService";

    @Test(groups = {"regression"},
          description = "Pact consumer test: GET /users/1 -> 200 JSON maps to User, pact file written")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Consumer contract for GET /users/1")
    public void contract_getUser_shouldSatisfyPactAndWriteFile() throws Exception {
        String outputDir = ConfigManager.getOrDefault("pact.output.dir", "target/pacts");
        Path pactFile = Path.of(outputDir, CONSUMER + "-" + PROVIDER + ".json");
        // Start from a clean slate so re-runs never hit a "cannot merge incompatible pacts" error.
        Files.deleteIfExists(pactFile);

        RequestResponsePact pact = ConsumerPactBuilder
                .consumer(CONSUMER)
                .hasPactWith(PROVIDER)
                .uponReceiving("a request for user 1")
                .path("/users/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .integerType("id", 1)
                        .stringType("name", "Rahul Mishra")
                        .stringType("email", "rahul@example.com")
                        .stringType("job", "SDET"))
                .toPact();

        MockProviderConfig config = MockProviderConfig.createDefault(PactSpecVersion.V3);
        AtomicReference<User> received = new AtomicReference<>();

        PactVerificationResult verification = runConsumerTest(pact, config, (mockServer, context) -> {
            ApiClient client = new ApiClient(mockServer.getUrl());
            Response response = client.get("/users/1");
            Assert.assertEquals(response.statusCode(), 200, "Mock server should honour the contract");
            User user = response.as(User.class);
            received.set(user);
            return null;
        });

        // The interaction was matched by the Pact mock server.
        Assert.assertTrue(verification instanceof PactVerificationResult.Ok,
                "Pact verification should be Ok but was: " + verification);

        // The response mapped cleanly onto the framework POJO.
        User user = received.get();
        Assert.assertNotNull(user, "User should have been deserialized from the mock response");
        Assert.assertEquals(user.getId(), 1);
        Assert.assertEquals(user.getName(), "Rahul Mishra");
        Assert.assertEquals(user.getEmail(), "rahul@example.com");
        Assert.assertEquals(user.getJob(), "SDET");

        // Persist the contract for the provider side / broker (V3 model = RequestResponsePact).
        // runConsumerTest already writes it on success; this makes the location explicit and
        // is a no-op merge since both sides are the same V3 pact.
        pact.write(outputDir, PactSpecVersion.V3);

        Assert.assertTrue(Files.exists(pactFile),
                "Pact file should be written to " + pactFile.toAbsolutePath());
        log.info("Pact file written: {}", pactFile.toAbsolutePath());
    }
}
