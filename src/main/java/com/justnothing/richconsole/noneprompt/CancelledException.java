package com.justnothing.richconsole.noneprompt;

/**
 * Exception thrown when the user cancels a prompt (Ctrl+C or Ctrl+Q).
 * Ported from noneprompt/CancelledError.
 */
public class CancelledException extends Exception {

    public CancelledException() {
        super("No answer selected!");
    }

    public CancelledException(String message) {
        super(message);
    }
}
