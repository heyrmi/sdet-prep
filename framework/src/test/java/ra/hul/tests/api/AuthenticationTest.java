package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

@Epic("API Automation")
@Feature("Authentication")
public class AuthenticationTest extends BaseApiTest {

    @Test(groups = {"smoke", "regression"},
          description = "Basic auth with valid credentials returns 200")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Basic Authentication")
    public void basicAuth_validCredentials_shouldReturn200() {
        Response response = apiClient.getWithAuth(
                Endpoints.API_BASIC_AUTH + "/user/passwd", "user", "passwd");
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("authenticated"));
        Assert.assertEquals(response.jsonPath().getString("user"), "user");
    }

    @Test(groups = {"regression"},
          description = "Basic auth with invalid credentials returns 401")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Basic Authentication")
    public void basicAuth_invalidCredentials_shouldReturn401() {
        Response response = apiClient.getWithAuth(
                Endpoints.API_BASIC_AUTH + "/user/passwd", "wrong", "creds");
        Assert.assertEquals(response.statusCode(), 401);
    }

    @Test(groups = {"regression"},
          description = "Unauthenticated request to protected endpoint returns 401")
    @Severity(SeverityLevel.NORMAL)
    @Story("Basic Authentication")
    public void basicAuth_noCredentials_shouldReturn401() {
        Response response = apiClient.get(Endpoints.API_BASIC_AUTH + "/user/passwd");
        Assert.assertEquals(response.statusCode(), 401);
    }
}
