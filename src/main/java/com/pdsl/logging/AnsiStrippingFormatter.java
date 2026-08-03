package com.pdsl.logging;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * A custom SimpleFormatter that strips out all ANSI escape codes (like colors)
 * before writing log messages to the destination.
 */
public class AnsiStrippingFormatter extends SimpleFormatter {

    private static final String ANSI_REGEX = "\\u001B\\[[;\\d]*[a-zA-Z]";

    @Override
    public synchronized String format(LogRecord record) {
        String formattedMessage = super.format(record);
        return formattedMessage.replaceAll(ANSI_REGEX, "");
    }
}
