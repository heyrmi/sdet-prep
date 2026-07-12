package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

import java.util.Map;

@Epic("API Automation")
@Feature("PUT/PATCH/DELETE Requests")
public class PutPatchDeleteTest extends BaseApiTest {

    @Test(groups = {"regression"},
          description = "PUT /put echoes the sent body")
    @Severity(SeverityLevel.NORMAL)
    @Story("PUT Request")
    public void put_shouldReturn200AndEchoBody() {
        Map<String, String> body = Map.of("name", "Updated", "status", "active");
        Response response = apiClient.put(Endpoints.API_PUT, body);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.name"), "Updated");
    }

    @Test(groups = {"regression"},
          description = "PATCH /patch echoes the sent body")
    @Severity(SeverityLevel.NORMAL)
    @Story("PATCH Request")
    public void patch_shouldReturn200AndEchoBody() {
        Map<String, String> body = Map.of("field", "patched-value");
        Response response = apiClient.patch(Endpoints.API_PATCH, body);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.field"), "patched-value");
    }

    @Test(groups = {"regression"},
          description = "DELETE /delete returns 200")
    @Severity(SeverityLevel.NORMAL)
    @Story("DELETE Request")
    public void delete_shouldReturn200() {
        Response response = apiClient.delete(Endpoints.API_DELETE);
        Assert.assertEquals(response.statusCode(), 200);
    }
}
