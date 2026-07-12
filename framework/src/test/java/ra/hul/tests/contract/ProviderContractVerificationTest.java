package ra.hul.tests.contract;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.DefaultPactReader;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import au.com.dius.pact.core.model.RequestResponsePact;
import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ra.hul.framework.api.client.ApiClient;
import ra.hul.framework.api.models.User;
import ra.hul.framework.core.config.ConfigManager;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, self-contained Pact <b>provider</b> verification.
 *
 * <p>Stands up a tiny embedded provider ({@link HttpServer} from the JDK) that serves the
 * contracted {@code GET /users/1} response, then loads the pact written to {@code pact.output.dir}
 * and replays every interaction against the embedded provider, asserting the real response
 * satisfies the contract (status + body deserializes to {@link User}). Fully offline.</p>
 *
 * <p>If the consumer pact has not been written yet (e.g. this class runs first), it is regenerated
 * so the test is independent.</p>
 */
@Epic("API Automation")
@Feature("Contract Testing (Pact JVM)")
public class ProviderContractVerificationTest {

    private static final Logger log = LogManager.getLogger(ProviderContractVerificationTest.class);

    private static final String CONSUMER = "FrameworkClient";
    private static final String PROVIDER = "UserService";
    private static final String PROVIDER_BODY =
            "{\"id\":1,\"name\":\"Rahul Mishra\",\"email\":\"rahul@example.com\",\"job\":\"SDET\"}";

    private HttpServer server;
    private String baseUrl;

    @BeforeClass(alwaysRun = true)
    public void startProvider() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/users/1", exchange -> {
            byte[] payload = PROVIDER_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        log.info("Embedded provider started at {}", baseUrl);
    }

    @AfterClass(alwaysRun = true)
    public void stopProvider() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test(groups = {"regression"},
          description = "Embedded provider satisfies every interaction in the consumer pact")
    @Severity(SeverityLevel.NORMAL)
    @Story("Provider verification against target/pacts")
    public void contract_embeddedProvider_shouldSatisfyPact() throws Exception {
        Path pactFile = ensurePactFile();
        Pact pact = DefaultPactReader.INSTANCE.loadPact(pactFile.toFile());

        List<Interaction> interactions = pact.getInteractions();
        Assert.assertFalse(interactions.isEmpty(), "Pact should contain at least one interaction");

        ApiClient client = new ApiClient(baseUrl);
        int verified = 0;
        for (Interaction interaction : interactions) {
            Assert.assertTrue(interaction instanceof RequestResponseInteraction,
                    "Expected a request/response interaction");
            RequestResponseInteraction rr = (RequestResponseInteraction) interaction;

            String method = rr.getRequest().getMethod();
            String path = rr.getRequest().getPath();
            int expectedStatus = rr.getResponse().getStatus();

            Assert.assertEquals(method, "GET", "This demo only replays GET interactions");
            Response response = client.get(path);

            Assert.assertEquals(response.statusCode(), expectedStatus,
                    "Provider status for " + method + " " + path + " must match the contract");

            User user = response.as(User.class);
            Assert.assertEquals(user.getId(), 1);
            Assert.assertNotNull(user.getName(), "Contract requires a name field");
            Assert.assertNotNull(user.getEmail(), "Contract requires an email field");
            Assert.assertNotNull(user.getJob(), "Contract requires a job field");
            verified++;
        }
        log.info("Provider verification passed for {} interaction(s) from {}", verified, pactFile);
    }

    /** Load the consumer pact from the output dir, regenerating it if this class runs first. */
    private Path ensurePactFile() {
        String outputDir = ConfigManager.getOrDefault("pact.output.dir", "target/pacts");
        Path pactFile = Path.of(outputDir, CONSUMER + "-" + PROVIDER + ".json");
        if (!Files.exists(pactFile)) {
            log.info("Pact file {} not found — regenerating from the DSL", pactFile);
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
            pact.write(outputDir, PactSpecVersion.V3);
        }
        return pactFile;
    }
}
