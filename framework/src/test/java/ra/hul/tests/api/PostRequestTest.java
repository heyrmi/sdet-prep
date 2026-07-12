package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ra.hul.framework.api.models.User;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.framework.data.FakerProvider;
import ra.hul.framework.data.UserFactory;
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
          description = "POST with POJO serialization (built via the datafaker UserFactory)")
    @Severity(SeverityLevel.NORMAL)
    @Story("POJO Serialization")
    public void post_withPojo_shouldSerializeAndEcho() {
        // Test data comes from the deterministic factory; overrides keep the assertion stable.
        User user = UserFactory.newUser()
                .withName("Rahul")
                .withEmail("rahul@test.com")
                .build();

        Response response = apiClient.post(Endpoints.API_POST, user);
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.name"), "Rahul");
        Assert.assertEquals(response.jsonPath().getString("json.email"), "rahul@test.com");
    }

    /** Data-driven users produced by the seeded factory — deterministic across runs. */
    @DataProvider(name = "factoryUsers")
    public Object[][] factoryUsers() {
        return new Object[][]{
                {UserFactory.newUser(FakerProvider.seeded(1)).withJob("SDET").build()},
                {UserFactory.newUser(FakerProvider.seeded(2)).withJob("QA Lead").build()},
                {UserFactory.newUser(FakerProvider.seeded(3)).withJob("Automation Architect").build()},
        };
    }

    @Test(dataProvider = "factoryUsers", groups = {"regression"},
          description = "POST factory-generated users; httpbin echoes the exact fields back")
    @Severity(SeverityLevel.NORMAL)
    @Story("Data-driven POST via factory")
    public void post_withFactoryUser_shouldEchoData(User user) {
        Response response = apiClient.post(Endpoints.API_POST, user);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("json.name"), user.getName());
        Assert.assertEquals(response.jsonPath().getString("json.email"), user.getEmail());
        Assert.assertEquals(response.jsonPath().getInt("json.id"), user.getId());
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
