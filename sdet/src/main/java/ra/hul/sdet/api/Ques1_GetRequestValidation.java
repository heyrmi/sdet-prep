package ra.hul.sdet.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * GET Request Validation - Hit a public REST API and validate the response.
 * Common SDET question: "Write a test to validate a GET endpoint's status, body, and headers."
 *
 * Target: https://reqres.in/api/users (free, no auth needed)
 */
public class Ques1_GetRequestValidation {

    static void main() {
        RestAssured.baseURI = "https://reqres.in";

        System.out.println("=== Test 1: GET /api/users?page=1 ===");
        given()
                .queryParam("page", 1)
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .body("page", equalTo(1))
                .body("data", hasSize(greaterThan(0)))
                .body("data[0].email", containsString("@"))
                .header("Content-Type", containsString("application/json"));
        System.out.println("PASSED: Status 200, body and headers validated.\n");

        System.out.println("=== Test 2: GET /api/users/2 - Single user ===");
        Response response =
                given()
                .when()
                        .get("/api/users/2")
                .then()
                        .statusCode(200)
                        .extract().response();

        String firstName = response.jsonPath().getString("data.first_name");
        String email = response.jsonPath().getString("data.email");
        System.out.printf("User: %s, Email: %s%n", firstName, email);
        System.out.println("PASSED: Single user fetched and extracted.\n");

        System.out.println("=== Test 3: GET /api/users/999 - Not found ===");
        given()
        .when()
                .get("/api/users/999")
        .then()
                .statusCode(404);
        System.out.println("PASSED: 404 returned for non-existent user.");
    }
}
