package ra.hul.tests.api;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.api.models.User;
import ra.hul.framework.core.constants.Endpoints;
import ra.hul.tests.base.BaseApiTest;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@Epic("API Automation")
@Feature("Schema Validation")
public class SchemaValidationTest extends BaseApiTest {

    @Test(groups = {"regression"},
          description = "POST user validates against JSON schema")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Contract Testing")
    public void postUser_shouldMatchJsonSchema() {
        User user = User.builder()
                .id(1)
                .name("Rahul")
                .email("rahul@test.com")
                .job("SDET")
                .build();

        Response response = apiClient.post(Endpoints.API_POST, user);
        Assert.assertEquals(response.statusCode(), 200);

        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/user-schema.json"));
    }
}
