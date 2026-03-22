package ra.hul.framework.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import ra.hul.framework.config.ConfigManager;

public class RetryAnalyzer implements IRetryAnalyzer {
    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);

    private int currentRetry = 0;
    private static final int MAX_RETRY = ConfigManager.getInt("retry.count");

    @Override
    public boolean retry(ITestResult result) {
        if (currentRetry < MAX_RETRY) {
            currentRetry++;
            log.warn("Retrying test: {} (attempt {}/{})",
                    result.getMethod().getMethodName(), currentRetry, MAX_RETRY);

            return true;
        }
        return false;
    }
}
