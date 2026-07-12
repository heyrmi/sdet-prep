package ra.hul.framework.api.client;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ra.hul.framework.core.config.ConfigManager;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * REST API client with automatic Allure request/response logging.
 */
public class ApiClient {

    private final RequestSpecification requestSpec;

    public ApiClient() {
        this(ConfigManager.get("api.base.url"));
    }

    public ApiClient(String baseUri) {
        int timeoutMs = ConfigManager.getIntOrDefault("api.timeout", 10) * 1000;

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .setConfig(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", timeoutMs)
                                .setParam("http.socket.timeout", timeoutMs)
                                .setParam("http.connection-manager.timeout", (long) timeoutMs)))
                .build();
    }

    public Response get(String endpoint) {
        return given().spec(requestSpec).get(endpoint).then().extract().response();
    }

    public Response getWithQueryParams(String endpoint, Map<String, ?> params) {
        return given().spec(requestSpec).queryParams(params).get(endpoint).then().extract().response();
    }

    public Response getWithAuth(String endpoint, String username, String password) {
        return given().spec(requestSpec)
                .auth().preemptive().basic(username, password)
                .get(endpoint).then().extract().response();
    }

    public Response post(String endpoint, Object body) {
        return given().spec(requestSpec).body(body).post(endpoint).then().extract().response();
    }

    public Response put(String endpoint, Object body) {
        return given().spec(requestSpec).body(body).put(endpoint).then().extract().response();
    }

    public Response patch(String endpoint, Object body) {
        return given().spec(requestSpec).body(body).patch(endpoint).then().extract().response();
    }

    public Response delete(String endpoint) {
        return given().spec(requestSpec).delete(endpoint).then().extract().response();
    }

    public Response getWithHeaders(String endpoint, Map<String, String> headers) {
        return given().spec(requestSpec).headers(headers).get(endpoint).then().extract().response();
    }
}
