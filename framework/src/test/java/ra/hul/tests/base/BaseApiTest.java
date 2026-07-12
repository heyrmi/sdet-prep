package ra.hul.tests.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;
import ra.hul.framework.api.client.ApiClient;

public class BaseApiTest {

    protected final Logger log = LogManager.getLogger(getClass());
    protected ApiClient apiClient;

    @BeforeClass(alwaysRun = true)
    public void setUpApiClient() {
        apiClient = new ApiClient();
    }
}
