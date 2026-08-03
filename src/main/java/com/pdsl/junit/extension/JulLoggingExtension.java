package com.pdsl.junit.extension;

import com.pdsl.logging.LogContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 Extension that captures current test metadata and stores it
 * in LogContext for java.util.logging to use in dynamic file sifting.
 */
public class JulLoggingExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        String testClassName = context.getRequiredTestClass().getSimpleName();
        String testMethodName = context.getRequiredTestMethod().getName();
        String displayName = context.getDisplayName();

        // Sanitize the display name to build a safe filename
        String sanitizedDisplayName = displayName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_")
                .trim();
        
        // Build the unique JUnit name
        String testId = String.format("%s_%s_%s", testClassName, testMethodName, sanitizedDisplayName);

        // Store the name in the LogContext
        LogContext.set(testId);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Prevent ThreadLocal memory leaks
        LogContext.remove();
    }
}
