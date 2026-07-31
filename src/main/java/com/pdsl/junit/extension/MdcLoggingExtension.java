package com.pdsl.junit.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

public class MdcLoggingExtension implements BeforeEachCallback, AfterEachCallback {

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

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Remove the key to prevent context leakage between threads or test executions
        MDC.remove(LoggingConstants.TEST_ID_KEY);
    }
}
