package ra.hul.sdet.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * POST Request Validation - Send a JSON body to create a resource and validate the response.
 * Common SDET question: "Send a POST with a body, validate the created resource echoes the
 * request fields and returns a generated id with 201 Created."
 *
 * Target: https://jsonplaceholder.typicode.com (free, no auth). NEEDS NETWORK.
 * Note: jsonplaceholder fakes writes - it does not persist. Assert only on echoed fields + id.
 */
public class Ques2_PostRequestValidation {

    static void main() {
        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        Map<String, Object> body = new HashMap<>();
        body.put("title", "SDET Prep");
        body.put("body", "Validating a POST request");
        body.put("userId", 42);

        System.out.println("=== POST /posts - create a resource ===");
        Response response =
                given()
                        .contentType(ContentType.JSON)
                        .body(body)
                .when()
                        .post("/posts")
                .then()
                        .statusCode(201)                       // Created
                        .contentType(ContentType.JSON)
                        .body("title", equalTo("SDET Prep"))   // echoed request field
                        .body("body", equalTo("Validating a POST request"))
                        .body("userId", equalTo(42))
                        .body("id", notNullValue())            // server-generated id
                        .extract().response();

        int createdId = response.jsonPath().getInt("id");
        System.out.printf("Created resource id=%d, title=%s%n",
                createdId, response.jsonPath().getString("title"));

        // Explicit self-check so the file verifies without -ea.
        boolean ok = createdId > 0
                && "SDET Prep".equals(response.jsonPath().getString("title"))
                && response.statusCode() == 201;
        System.out.println(ok
                ? "PASSED: 201 Created, echoed fields and generated id validated."
                : "FAILED: unexpected create response.");
    }
}
