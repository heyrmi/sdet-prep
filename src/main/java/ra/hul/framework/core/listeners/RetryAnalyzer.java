package ra.hul.framework.core.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import ra.hul.framework.core.config.ConfigManager;

public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private int retryCount = 0;
    private final int maxRetryCount = ConfigManager.getIntOrDefault("retry.count", 1);

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount) {
            retryCount++;
            log.warn("Retrying test: {} (attempt {}/{})",
                    result.getMethod().getMethodName(), retryCount, maxRetryCount);
            return true;
        }
        return false;
    }
}
