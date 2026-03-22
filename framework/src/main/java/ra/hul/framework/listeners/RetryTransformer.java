package ra.hul.framework.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Auto-applies RetryAnalyzer to all @Test methods without manual annotation.
 * <p>
 * Registered as a listener in testng.xml. This is the correct way to
 * wire retry logic — IAnnotationTransformer is a suite-level listener,
 * unlike IRetryAnalyzer which must be applied per-test.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
