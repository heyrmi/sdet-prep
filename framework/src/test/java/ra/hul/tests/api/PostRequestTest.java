package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.api.models.User;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

import java.util.Map;

@Epic("API Automation")
@Feature("POST Requests")
public class PostRequestTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"},
          description = "POST /post with JSON body returns 200 and echoes data")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Basic POST")
    public void post_withJsonBody_shouldReturn200AndEchoData() {
        Map<String, String> body = Map.of("name", "Rahul", "role", "SDET");
        Response response = apiClient.post(Endpoints.API_POST, body);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.name"), "Rahul");
        Assert.assertEquals(response.jsonPath().getString("json.role"), "SDET");
    }

    @Test(groups = {"regression"},
          description = "POST with POJO serialization")
    @Severity(SeverityLevel.NORMAL)
    @Story("POJO Serialization")
    public void post_withPojo_shouldSerializeAndEcho() {
        User user = User.builder()
                .id(1)
                .name("Rahul")
                .email("rahul@test.com")
                .job("SDET")
                .build();

        Response response = apiClient.post(Endpoints.API_POST, user);
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.name"), "Rahul");
        Assert.assertEquals(response.jsonPath().getString("json.email"), "rahul@test.com");
    }

    @Test(groups = {"regression"},
          description = "POST with empty body returns 200")
    @Severity(SeverityLevel.MINOR)
    @Story("Edge Cases")
    public void post_emptyBody_shouldReturn200() {
        Response response = apiClient.post(Endpoints.API_POST, "{}");
        Assert.assertEquals(response.statusCode(), 200);
    }
}
