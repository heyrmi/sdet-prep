package ra.hul.tests.api;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ra.hul.framework.api.ApiClient;

import java.util.HashMap;
import java.util.Map;

/**
 * API tests against httpbin.org
 * <p>
 * httpbin echoes back whatever you send — perfect for testing
 * framework capabilities: serialization, headers, auth, status codes.
 * <p>
 * Key httpbin endpoints used:
 * /get         → echoes query params and headers
 * /post        → echoes posted JSON body
 * /put         → echoes put body
 * /delete      → echoes delete request
 * /status/xxx  → returns specific HTTP status code
 * /basic-auth  → tests basic authentication
 * /headers     → returns all sent headers
 * /delay/n     → delays response by n seconds
 * /json        → returns a sample JSON payload
 */
public class HttpBinApiTest {

    private ApiClient apiClient;

    @BeforeClass
    public void setup() {
        apiClient = new ApiClient();
    }

    // ============================
    // GET Tests
    // ============================

    @Test(description = "GET /get should return 200 with request details")
    public void get_shouldReturn200WithEchoedData() {
        Response response = apiClient.get("/get");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertNotNull(response.jsonPath().getString("url"),
                "Response should contain the request URL");
        Assert.assertNotNull(response.jsonPath().getString("origin"),
                "Response should contain the origin IP");
    }

    @Test(description = "GET with query parameters should echo them back")
    public void get_withQueryParams_shouldEchoParams() {
        Map<String, String> params = new HashMap<>();
        params.put("search", "hotstar");
        params.put("page", "1");

        Response response = apiClient.getWithQueryParams("/get", params);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("args.search"), "hotstar");
        Assert.assertEquals(response.jsonPath().getString("args.page"), "1");
    }

    @Test(description = "GET /json should return valid sample JSON")
    public void getJson_shouldReturnSamplePayload() {
        Response response = apiClient.get("/json");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertNotNull(response.jsonPath().getString("slideshow"),
                "Response should contain 'slideshow' object");
        Assert.assertNotNull(response.jsonPath().getString("slideshow.title"),
                "Slideshow should have a title");
    }

    // ============================
    // POST Tests
    // ============================

    @Test(description = "POST should echo back the sent JSON body")
    public void post_withJsonBody_shouldEchoBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Rahul Mishra");
        body.put("role", "Senior SDET");
        body.put("company", "JioStar");

        Response response = apiClient.post("/post", body);

        Assert.assertEquals(response.statusCode(), 200);
        // httpbin returns sent data in "json" field
        Assert.assertEquals(response.jsonPath().getString("json.name"), "Rahul Mishra");
        Assert.assertEquals(response.jsonPath().getString("json.role"), "Senior SDET");
        Assert.assertEquals(response.jsonPath().getString("json.company"), "JioStar");
    }

    @Test(description = "POST should reflect correct Content-Type header")
    public void post_shouldSendCorrectContentType() {
        Map<String, Object> body = Map.of("key", "value");

        Response response = apiClient.post("/post", body);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(
                response.jsonPath().getString("headers.Content-Type").contains("application/json"),
                "Content-Type should be application/json");
    }

    // ============================
    // PUT & DELETE Tests
    // ============================

    @Test(description = "PUT should echo back the updated body")
    public void put_withJsonBody_shouldEchoBody() {
        Map<String, Object> body = Map.of("name", "Rahul", "job", "Lead SDET");

        Response response = apiClient.put("/put", body);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.name"), "Rahul");
        Assert.assertEquals(response.jsonPath().getString("json.job"), "Lead SDET");
    }

    @Test(description = "DELETE should return 200")
    public void delete_shouldReturn200() {
        Response response = apiClient.delete("/delete");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertNotNull(response.jsonPath().getString("url"));
    }

    // ============================
    // Status Code Tests
    // ============================

    @Test(description = "GET /status/200 should return 200 OK")
    public void status200_shouldReturnOk() {
        Response response = apiClient.get("/status/200");
        Assert.assertEquals(response.statusCode(), 200);
    }

    @Test(description = "GET /status/404 should return Not Found")
    public void status404_shouldReturnNotFound() {
        Response response = apiClient.get("/status/404");
        Assert.assertEquals(response.statusCode(), 404);
    }

    @Test(description = "GET /status/500 should return Internal Server Error")
    public void status500_shouldReturnServerError() {
        Response response = apiClient.get("/status/500");
        Assert.assertEquals(response.statusCode(), 500);
    }

    @Test(description = "GET /status/401 should return Unauthorized")
    public void status401_shouldReturnUnauthorized() {
        Response response = apiClient.get("/status/401");
        Assert.assertEquals(response.statusCode(), 401);
    }

    // ============================
    // Authentication Tests
    // ============================

    @Test(description = "Basic auth with valid credentials should return 200")
    public void basicAuth_validCredentials_shouldReturn200() {
        Response response = apiClient.getWithAuth(
                "/basic-auth/testuser/testpass", "testuser", "testpass");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("authenticated"),
                "Should be authenticated with valid credentials");
        Assert.assertEquals(response.jsonPath().getString("user"), "testuser");
    }

    @Test(description = "Basic auth with invalid credentials should return 401")
    public void basicAuth_invalidCredentials_shouldReturn401() {
        Response response = apiClient.getWithAuth(
                "/basic-auth/testuser/testpass", "wrong", "wrong");

        Assert.assertEquals(response.statusCode(), 401);
    }

    // ============================
    // Headers Test
    // ============================

    @Test(description = "Verify request headers are echoed back")
    public void headers_shouldEchoRequestHeaders() {
        Response response = apiClient.get("/headers");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(
                response.jsonPath().getString("headers.Accept").contains("application/json"),
                "Accept header should be present");
    }

    // ============================
    // Response Time / Performance Test
    // ============================

    @Test(description = "GET /get should respond within 3 seconds")
    public void get_shouldRespondWithinSLA() {
        Response response = apiClient.get("/get");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.time() < 3000,
                "Response time " + response.time() + "ms exceeded 3000ms SLA");
    }

    // ============================
    // Response Chaining Test (Pattern Demo)
    // ============================

    @Test(description = "Chain: POST data → verify it was echoed correctly")
    public void responseChaining_postAndVerify() {
        // Step 1: POST data
        Map<String, Object> userData = Map.of(
                "name", "Rahul",
                "email", "rahul@jiostar.com",
                "role", "Senior SDET"
        );

        Response postResponse = apiClient.post("/post", userData);
        Assert.assertEquals(postResponse.statusCode(), 200);

        // Step 2: Extract and verify echoed data
        String echoedName = postResponse.jsonPath().getString("json.name");
        String echoedEmail = postResponse.jsonPath().getString("json.email");

        Assert.assertEquals(echoedName, "Rahul");
        Assert.assertEquals(echoedEmail, "rahul@jiostar.com");

        // Step 3: Use extracted data in next request
        Map<String, String> queryParams = Map.of("user", echoedName);
        Response getResponse = apiClient.getWithQueryParams("/get", queryParams);

        Assert.assertEquals(getResponse.jsonPath().getString("args.user"), "Rahul");
    }
}