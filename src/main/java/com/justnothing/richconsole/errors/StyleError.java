package com.justnothing.richconsole.errors;

/**
 * An error in styles.
 */
public class StyleError extends RuntimeException {
    public StyleError(String message) {
        super(message);
    }
}
