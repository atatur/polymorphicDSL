package com.pdsl.junit.jupiter.extension.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

/**
 * A JUnit 5 extension that adds a unique identifier to the logging context (MDC) for each test.
 *
 * <p>This extension puts a unique test ID into the SLF4J Mapped Diagnostic Context (MDC) before each
 * test is run, and removes it after the test completes. This allows for structured logging and
 * makes it easier to trace logs for a specific test execution, especially in parallel test runs.
 *
 * <p>The test ID is constructed using the test class name, test method name, and the display name of
 * the test.
 */
public class MdcLoggingExtension implements BeforeEachCallback, AfterEachCallback {

    /**
     * Called before each test method execution.
     *
     * <p>This method constructs a unique test identifier and puts it into the MDC.
     *
     * @param context the extension context for the current test
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        String testClassName = context.getRequiredTestClass().getSimpleName();
        String testMethodName = context.getRequiredTestMethod().getName();
        // For @TestTemplate or parameterized tests, getDisplayName() returns the unique invocation name
        String displayName = context.getDisplayName();

        // Sanitize the display name to construct a safe filename
        String sanitizedDisplayName = displayName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_")
                .trim();
        // Construct a unique Test ID
        String testId = String.format("%s_%s_%s", testClassName, testMethodName, sanitizedDisplayName);

        // Put the unique identifier into MDC
        MDC.put(LoggingConstants.TEST_ID_KEY, testId);
    }

    /**
     * Called after each test method execution.
     *
     * <p>This method removes the test identifier from the MDC to prevent context leakage.
     *
     * @param context the extension context for the current test
     * @throws Exception if an error occurs
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Remove the key to prevent context leakage between threads or test executions
        MDC.remove(LoggingConstants.TEST_ID_KEY);
    }
}
