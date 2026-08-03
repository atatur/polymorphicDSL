package com.pdsl.logging;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A custom Handler for java.util.logging that dynamically routes log records
 * to separate files based on the active JUnit test name stored in LogContext.
 */
public class JUnitNameSiftingHandler extends Handler {

    private final String logDirectoryPath;
    private final ConcurrentHashMap<String, FileHandler> fileHandlers = new ConcurrentHashMap<>();

    public JUnitNameSiftingHandler(String logDirectoryPath) {
        this.logDirectoryPath = logDirectoryPath;
        File dir = new File(logDirectoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        // Get the active test name from LogContext
        String testId = LogContext.get();
        String safeTestId = testId.replaceAll("[^a-zA-Z0-9-_]", "_");

        // Locate or create a FileHandler for this specific testId
        FileHandler threadHandler = fileHandlers.computeIfAbsent(safeTestId, name -> {
            try {
                String logFilePath = new File(logDirectoryPath, name + ".log").getAbsolutePath();
                FileHandler fh = new FileHandler(logFilePath, false);
                
                if (this.getFormatter() != null) {
                    fh.setFormatter(this.getFormatter());
                } else {
                    fh.setFormatter(new java.util.logging.SimpleFormatter());
                }
                return fh;
            } catch (IOException e) {
                reportError("Failed to create FileHandler for JUnit test: " + name, e, ErrorManager.OPEN_FAILURE);
                return null;
            }
        });

        if (threadHandler != null) {
            threadHandler.publish(record);
        }
    }

    @Override
    public void flush() {
        for (FileHandler fh : fileHandlers.values()) {
            fh.flush();
        }
    }

    @Override
    public void close() throws SecurityException {
        for (FileHandler fh : fileHandlers.values()) {
            fh.close();
        }
        fileHandlers.clear();
    }
}
