package com.pdsl.logging;

/**
 * ThreadLocal container to track the active JUnit test name for the executing thread.
 */
public final class LogContext {
    private static final ThreadLocal<String> currentTestId = ThreadLocal.withInitial(() -> "common");

    private LogContext() {
    }

    public static String get() {
        return currentTestId.get();
    }

    public static void set(String testId) {
        currentTestId.set(testId);
    }

    public static void remove() {
        currentTestId.remove();
    }
}
