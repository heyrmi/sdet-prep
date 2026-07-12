package ra.hul.sdet.api;

import io.restassured.RestAssured;

import static io.restassured.RestAssured.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.hamcrest.Matchers.*;

/**
 * JSON Schema Validation - Validate an API response against an inline JSON Schema.
 * Common SDET question: "Validate that a response conforms to a schema - required fields
 * present, correct data types - and prove a bad schema fails."
 *
 * Target: https://jsonplaceholder.typicode.com (free, no auth). NEEDS NETWORK.
 * Uses io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema with an inline schema
 * string (no external .json file needed).
 */
public class Ques4_JsonSchemaValidation {

    // Draft-04 schema describing a /posts/{id} object.
    private static final String POST_SCHEMA = """
        {
          "$schema": "http://json-schema.org/draft-04/schema#",
          "type": "object",
          "required": ["userId", "id", "title", "body"],
          "properties": {
            "userId": { "type": "integer" },
            "id":     { "type": "integer" },
            "title":  { "type": "string" },
            "body":   { "type": "string" }
          }
        }
        """;

    // A deliberately wrong schema: expects a top-level "type": "array".
    private static final String WRONG_SCHEMA = """
        {
          "$schema": "http://json-schema.org/draft-04/schema#",
          "type": "array"
        }
        """;

    static void main() {
        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        // Positive case: object response matches the object schema.
        System.out.println("=== Positive: /posts/1 matches inline schema ===");
        given()
        .when()
                .get("/posts/1")
        .then()
                .statusCode(200)
                .body(matchesJsonSchema(POST_SCHEMA));
        System.out.println("PASSED: response conforms to JSON schema (required fields + types).");

        // Negative case: the same response must NOT match an array schema.
        System.out.println("=== Negative: /posts/1 must fail the wrong (array) schema ===");
        boolean failedAsExpected;
        try {
            given()
            .when()
                    .get("/posts/1")
            .then()
                    .body(matchesJsonSchema(WRONG_SCHEMA));
            failedAsExpected = false; // should not reach here
        } catch (AssertionError expected) {
            failedAsExpected = true;
        }
        System.out.println(failedAsExpected
                ? "PASSED: schema violation correctly detected for negative case."
                : "FAILED: invalid schema unexpectedly passed.");
    }
}
