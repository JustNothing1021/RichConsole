package com.justnothing.richconsole.errors;

/**
 * An error in console operation.
 */
public class ConsoleError extends RuntimeException {
    public ConsoleError(String message) {
        super(message);
    }

    public ConsoleError(String message, Throwable cause) {
        super(message, cause);
    }
}
