package ra.hul.framework.performance.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * POST load test against httpbin.org.
 * Minimal load with JSON payload.
 */
public class HttpBinPostSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://httpbin.org")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder postScenario = scenario("POST /post Load Test")
            .exec(
                    http("POST Request")
                            .post("/post")
                            .body(StringBody("{\"name\": \"gatling-user\", \"framework\": \"sdet-framework\"}"))
                            .check(status().is(200))
                            .check(jsonPath("$.json.name").is("gatling-user"))
            )
            .pause(1, 2);

    {
        setUp(
                postScenario.injectOpen(
                        atOnceUsers(2),
                        rampUsers(8).during(15)
                )
        ).protocols(httpProtocol)
         .assertions(
                 global().responseTime().percentile3().lt(3000),
                 global().failedRequests().percent().lt(5.0)
         );
    }
}
