package com.justnothing.richconsole.console;

/**
 * Exception raised when capture result is accessed before the capture context is closed.
 * Ported from rich/console.py CaptureError.
 */
public class CaptureError extends RuntimeException {
    public CaptureError(String message) {
        super(message);
    }
}
