package com.pdsl.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A default, Thread-Safe OutputStream used for TestExecutors to write output to.
 *
 * <p>Using System.out directly is a security vulnerability that has lead to several exploits in Java.
 * This class is backed by an SLF4J logger to avoid the security issue and to support MDC-based log routing
 * in multithreaded test executions.
 */
public final class PdslThreadSafeOutputStream extends OutputStream {

    private static final Logger logger = LoggerFactory.getLogger(PdslThreadSafeOutputStream.class);

    // The internal memory for the written bytes.
    // StringBuilder is asynchronized and faster than StringBuffer, but is made thread safe by being local to each thread.
    private final ThreadLocal<StringBuilder> mem = ThreadLocal.withInitial(StringBuilder::new);

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
}
