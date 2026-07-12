package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.core.config.ConfigManager;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

@Epic("API Automation")
@Feature("Response Time SLA")
public class ResponseTimeTest extends BaseApiTest {

    @Test(groups = {"regression"},
          description = "GET /get response time should be within SLA")
    @Severity(SeverityLevel.NORMAL)
    @Story("Performance SLA")
    public void get_responseTime_shouldBeWithinSla() {
        long slaMs = ConfigManager.getLongOrDefault("api.sla.ms", 5000);

        Response response = apiClient.get(Endpoints.API_GET);
        long responseTime = response.getTime();

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(responseTime < slaMs,
                "Response time " + responseTime + "ms exceeded SLA of " + slaMs + "ms");
    }

    @Test(groups = {"regression"},
          description = "POST /post response time should be within SLA")
    @Severity(SeverityLevel.NORMAL)
    @Story("Performance SLA")
    public void post_responseTime_shouldBeWithinSla() {
        long slaMs = ConfigManager.getLongOrDefault("api.sla.ms", 5000);

        Response response = apiClient.post(Endpoints.API_POST, "{}");
        long responseTime = response.getTime();

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(responseTime < slaMs,
                "Response time " + responseTime + "ms exceeded SLA of " + slaMs + "ms");
    }
}
