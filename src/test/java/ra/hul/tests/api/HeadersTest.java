package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

import java.util.Map;

@Epic("API Automation")
@Feature("Headers")
public class HeadersTest extends BaseApiTest {

    @Test(groups = {"regression"},
          description = "Custom headers are echoed back by /headers")
    @Severity(SeverityLevel.NORMAL)
    @Story("Custom Headers")
    public void headers_customHeaders_shouldBeEchoed() {
        Response response = apiClient.getWithHeaders(Endpoints.API_HEADERS,
                Map.of("X-Custom-Header", "TestValue", "X-Framework", "SDET"));

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(
                response.jsonPath().getString("headers.X-Custom-Header"), "TestValue");
        Assert.assertEquals(
                response.jsonPath().getString("headers.X-Framework"), "SDET");
    }

    @Test(groups = {"regression"},
          description = "Content-Type header is sent correctly")
    @Severity(SeverityLevel.MINOR)
    @Story("Default Headers")
    public void headers_contentType_shouldBeJson() {
        Response response = apiClient.get(Endpoints.API_HEADERS);
        Assert.assertEquals(response.statusCode(), 200);
        String contentType = response.jsonPath().getString("headers.Accept");
        Assert.assertTrue(contentType.contains("application/json"));
    }
}
