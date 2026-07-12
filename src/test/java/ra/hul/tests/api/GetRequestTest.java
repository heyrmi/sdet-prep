package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

import java.util.Map;

@Epic("API Automation")
@Feature("GET Requests")
public class GetRequestTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"},
          description = "GET /get returns 200 with URL in response")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Basic GET")
    public void get_shouldReturn200() {
        Response response = apiClient.get(Endpoints.API_GET);
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertNotNull(response.jsonPath().getString("url"));
    }

    @Test(groups = {"regression"},
          description = "GET with query params echoes them back")
    @Severity(SeverityLevel.NORMAL)
    @Story("Query Parameters")
    public void get_withQueryParams_shouldEchoParams() {
        Response response = apiClient.getWithQueryParams(Endpoints.API_GET,
                Map.of("framework", "selenium", "version", "4"));
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("args.framework"), "selenium");
        Assert.assertEquals(response.jsonPath().getString("args.version"), "4");
    }

    @Test(groups = {"regression"},
          description = "GET /json returns valid JSON body")
    @Severity(SeverityLevel.NORMAL)
    @Story("JSON Response")
    public void get_json_shouldReturnValidJsonBody() {
        Response response = apiClient.get(Endpoints.API_JSON);
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertNotNull(response.jsonPath().get("slideshow"));
    }
}
