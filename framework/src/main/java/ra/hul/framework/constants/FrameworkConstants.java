package ra.hul.framework.constants;

public class FrameworkConstants {

    // Prevent Instantiation (since it is a utility class)
    private FrameworkConstants() {
    }

    // Timeouts defaults
    public static final int EXPLICIT_WAIT = 10;
    public static final int PAGE_LOAD_TIMEOUT = 30;
    public static final int FLUENT_WAIT = 30;
    public static final int FLUENT_WAIT_POLL = 500;
    public static final int IMPLICIT_WAIT = 0; // mixing implicit wait and explicit wait caused unpredictable behavior

    // Paths
    public static final String CONFIG_PATH = "src/test/resources/config.properties";
    public static final String EXTENT_REPORT_PATH = "target/extent-report.html";
    public static final String SCREENSHOT_PATH = "target/screenshots/";
    public static final String SCHEMA_PATH = "schemas/";

    // Browser Types (can also make this enum but for simplicity keeping them constants)
    public static final String CHROME = "chrome";
    public static final String FIREFOX = "firefox";
    public static final String EDGE = "edge";
}
