package com.pdsl.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A default, Thread-Safe OutputStream used for TestExecutors to write output to.
 *
 * <p>Using System.out directly is a security vulnerability that has lead to several exploits in Java.
 * This class is backed by a java.util.logging.Logger to avoid the security issue and to support thread-safe
 * routing in multithreaded test executions.
 */
public final class PdslThreadSafeOutputStream extends OutputStream {

    private static final Logger logger = Logger.getLogger(PdslThreadSafeOutputStream.class.getName());

    // The internal memory for the written bytes.
    // StringBuilder is asynchronized and faster than StringBuffer, but is made thread safe by being local to each thread.
    private final ThreadLocal<StringBuilder> mem = ThreadLocal.withInitial(StringBuilder::new);

    static {
        configureLogger(logger);
    }

    @Override
    public void write(final int b) {
        char c = (char) b;
        mem.get().append(c);
        if (c == '\n') {
            flush();
        }
    }

    @Override
    public void write(byte[] bytes, int start, int stop) {
        String message = new String(bytes);
        // Multibyte characters may require us to have an earlier stop point
        //TODO: This is a bug. Find a way to handle character encoding instead of assuming
        // the caller wants to write the full array logged
        message = new String(bytes).substring(start, Math.min(stop, message.length()));
        logger.info(message);
    }

    @Override
    public void flush() {
        StringBuilder sb = mem.get();
        if (!sb.isEmpty()) {
            String message = sb.toString();
            // Remove trailing newline since loggers usually append newlines themselves
            if (message.endsWith("\n")) {
                message = message.substring(0, message.length() - 1);
            }
            if (!message.isEmpty()) {
                logger.info(message);
            }
            sb.setLength(0);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        mem.remove();
        super.close();
    }

    public static void configureLogger(Logger logger) {
        // Disable sending logs to parent handlers to prevent duplicate output
        logger.setUseParentHandlers(false);

        // Create Console Handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format("%s", lr.getMessage());
            }
        });

        // Assign our custom ANSI stripping formatter
        consoleHandler.setFormatter(new AnsiStrippingFormatter());

        // Attach the handler to the logger
        logger.addHandler(consoleHandler);

        // Create and register a JUnitNameSiftingHandler to write logs of each thread to separate files
        String logDir = System.getProperty("pdsl.test.logs.dir", "/tmp/test-logs");
        JUnitNameSiftingHandler siftingHandler = new JUnitNameSiftingHandler(logDir);
        siftingHandler.setFormatter(new AnsiStrippingFormatter());
        logger.addHandler(siftingHandler);
    }
}
