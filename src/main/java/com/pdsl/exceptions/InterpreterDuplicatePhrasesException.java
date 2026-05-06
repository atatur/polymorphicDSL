package com.pdsl.exceptions;

/**
 * Exception thrown when an interpreter encounters duplicate phrases in a case of NO_DUPLICATES_PHRASES interpreter constraint.
 *
 * <p>In the context of PDSL, this typically occurs when the same phrase is
 * processed multiple times by the same interpreter in a way that is not
 * allowed by the current configuration.</p>
 */
public class InterpreterDuplicatePhrasesException extends RuntimeException {

    /**
     * Constructs a new InterpreterDuplicatePhrasesException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public InterpreterDuplicatePhrasesException(String message) {
        super(message);
    }
}
