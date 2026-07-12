package ra.hul.framework.core.constants;

import ra.hul.framework.core.config.ConfigManager;

public final class TimeoutConstants {

    private TimeoutConstants() {
    }

    public static final int EXPLICIT_WAIT = ConfigManager.getIntOrDefault("explicit.wait", 15);
    public static final int FLUENT_WAIT = ConfigManager.getIntOrDefault("fluent.wait", 30);
    public static final int POLLING_TIME = ConfigManager.getIntOrDefault("polling.time", 500);
    public static final int PAGE_LOAD_TIMEOUT = ConfigManager.getIntOrDefault("page.load.timeout", 30);
    public static final int API_TIMEOUT = ConfigManager.getIntOrDefault("api.timeout", 10);
}
