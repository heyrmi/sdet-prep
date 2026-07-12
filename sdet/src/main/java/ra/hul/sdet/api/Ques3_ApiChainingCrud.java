package ra.hul.sdet.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * API Chaining (CRUD) - Run a full POST -> GET -> PUT -> DELETE cycle, feeding each step
 * from the previous response.
 * Common SDET question: "Implement a CRUD cycle where an id is extracted via jsonPath and
 * reused across the chain, validating state at every step."
 *
 * Target: https://jsonplaceholder.typicode.com (free, no auth). NEEDS NETWORK.
 * Note: jsonplaceholder fakes writes. POST returns id=101; GET on an existing id (we use 1) is
 * validated for the READ step. Assertions stay on stable/echoed fields, not persistence.
 */
public class Ques3_ApiChainingCrud {

    static void main() {
        RestAssured.baseURI = "https://jsonplaceholder.typicode.com";

        // 1) CREATE -----------------------------------------------------------
        Map<String, Object> newPost = new HashMap<>();
        newPost.put("title", "chain-create");
        newPost.put("body", "step one");
        newPost.put("userId", 7);

        Response created =
                given().contentType(ContentType.JSON).body(newPost)
                .when().post("/posts")
                .then().statusCode(201)
                        .body("title", equalTo("chain-create"))
                        .extract().response();
        int newId = created.jsonPath().getInt("id");
        System.out.printf("CREATE: 201, new id extracted via jsonPath = %d%n", newId);

        // 2) READ -------------------------------------------------------------
        // The fake API won't serve id 101, so READ a known existing resource (id 1).
        int readId = 1;
        Response fetched =
                given()
                .when().get("/posts/{id}", readId)
                .then().statusCode(200)
                        .body("id", equalTo(readId))
                        .body("userId", notNullValue())
                        .extract().response();
        System.out.printf("READ: 200, fetched id=%d title=%s%n",
                fetched.jsonPath().getInt("id"), fetched.jsonPath().getString("title"));

        // 3) UPDATE (PUT) -----------------------------------------------------
        Map<String, Object> update = new HashMap<>();
        update.put("id", readId);
        update.put("title", "chain-updated");
        update.put("body", "step three");
        update.put("userId", 7);

        Response updated =
                given().contentType(ContentType.JSON).body(update)
                .when().put("/posts/{id}", readId)
                .then().statusCode(200)
                        .body("title", equalTo("chain-updated"))  // echoed change
                        .body("id", equalTo(readId))
                        .extract().response();
        System.out.printf("UPDATE: 200, title now = %s%n", updated.jsonPath().getString("title"));

        // 4) DELETE -----------------------------------------------------------
        given()
        .when().delete("/posts/{id}", readId)
        .then().statusCode(200);
        System.out.printf("DELETE: 200, resource id=%d removed%n", readId);

        boolean ok = newId > 0
                && fetched.jsonPath().getInt("id") == readId
                && "chain-updated".equals(updated.jsonPath().getString("title"));
        System.out.println(ok
                ? "PASSED: CRUD chain POST -> GET -> PUT -> DELETE validated at each step."
                : "FAILED: CRUD chain assertion mismatch.");
    }
}
