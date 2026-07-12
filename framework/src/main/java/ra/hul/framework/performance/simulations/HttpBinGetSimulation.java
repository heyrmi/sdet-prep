package ra.hul.framework.performance.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * GET load test against httpbin.org.
 * Minimal load: ramp 1-5 users, sustain, ramp down.
 * Goal is to learn Gatling, not stress the target.
 */
public class HttpBinGetSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://httpbin.org")
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Performance-Test");

    ScenarioBuilder getScenario = scenario("GET /get Load Test")
            .exec(
                    http("GET Request")
                            .get("/get")
                            .check(status().is(200))
                            .check(jsonPath("$.url").exists())
            )
            .pause(1, 3);

    {
        setUp(
                getScenario.injectOpen(
                        rampUsersPerSec(1).to(5).during(10),
                        constantUsersPerSec(5).during(20),
                        rampUsersPerSec(5).to(1).during(10)
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().max().lt(5000),
                        global().successfulRequests().percent().gt(95.0)
                );
    }
}
