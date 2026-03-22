package ra.hul.framework.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ra.hul.framework.config.ConfigManager;

/**
 * Centralized API client built on RestAssured.
 * <p>
 * Configured for httpbin.org — a request/response testing service.
 * httpbin echoes back what you send, making it perfect for framework demos.
 * <p>
 * Key httpbin endpoints:
 * GET  /get          → returns query params + headers
 * POST /post         → returns posted body
 * PUT  /put          → returns put body
 * DELETE /delete     → returns delete confirmation
 * GET  /status/{code}→ returns specific status code
 * GET  /delay/{n}    → delays response by n seconds (test timeouts)
 * GET  /basic-auth/{user}/{pass} → tests basic auth
 * GET  /headers      → returns request headers
 * GET  /json         → returns sample JSON
 */
public class ApiClient {

    private static final Logger log = LogManager.getLogger(ApiClient.class);
    private final RequestSpecification requestSpec;

    public ApiClient() {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigManager.get("api.base.url"))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    /**
     * Overloaded constructor for custom base URI (useful for testing different services)
     */
    public ApiClient(String baseUri) {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    public Response get(String endpoint) {
        log.info("GET {}", endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .extract().response();
    }

    public Response getWithAuth(String endpoint, String username, String password) {
        log.info("GET {} with basic auth", endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .auth().preemptive().basic(username, password)
                .when()
                .get(endpoint)
                .then()
                .extract().response();
    }

    public Response getWithQueryParams(String endpoint, java.util.Map<String, String> params) {
        log.info("GET {} with params: {}", endpoint, params);
        return RestAssured.given()
                .spec(requestSpec)
                .queryParams(params)
                .when()
                .get(endpoint)
                .then()
                .extract().response();
    }

    public Response post(String endpoint, Object body) {
        log.info("POST {}", endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract().response();
    }

    public Response put(String endpoint, Object body) {
        log.info("PUT {}", endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract().response();
    }

    public Response delete(String endpoint) {
        log.info("DELETE {}", endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .extract().response();
    }
}