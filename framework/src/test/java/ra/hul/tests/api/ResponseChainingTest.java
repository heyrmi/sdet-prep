package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

import java.util.Map;

@Epic("API Automation")
@Feature("Response Chaining")
public class ResponseChainingTest extends BaseApiTest {

    @Test(groups = {"regression"},
          description = "Chain POST -> extract data -> GET with extracted data")
    @Severity(SeverityLevel.NORMAL)
    @Story("Request Chaining")
    public void responseChaining_postThenGetWithExtractedData() {
        // Step 1: POST to create data
        Response postResponse = apiClient.post(Endpoints.API_POST,
                Map.of("name", "Rahul", "role", "SDET"));
        String extractedName = postResponse.jsonPath().getString("json.name");
        Assert.assertEquals(extractedName, "Rahul");

        // Step 2: Use extracted value in next request
        Response getResponse = apiClient.getWithQueryParams(Endpoints.API_GET,
                Map.of("user", extractedName));
        Assert.assertEquals(getResponse.statusCode(), 200);
        Assert.assertEquals(getResponse.jsonPath().getString("args.user"), "Rahul");
    }

    @Test(groups = {"regression"},
          description = "Chain multiple requests and validate data flow")
    @Severity(SeverityLevel.NORMAL)
    @Story("Request Chaining")
    public void responseChaining_multiStep_shouldMaintainDataIntegrity() {
        // Step 1: POST
        Response step1 = apiClient.post(Endpoints.API_POST,
                Map.of("step", "1", "token", "abc123"));
        String token = step1.jsonPath().getString("json.token");

        // Step 2: GET with token as query param
        Response step2 = apiClient.getWithQueryParams(Endpoints.API_GET,
                Map.of("token", token));
        Assert.assertEquals(step2.jsonPath().getString("args.token"), "abc123");

        // Step 3: PUT with updated data
        Response step3 = apiClient.put(Endpoints.API_PUT,
                Map.of("token", token, "step", "3", "status", "completed"));
        Assert.assertEquals(step3.jsonPath().getString("json.status"), "completed");
    }
}
