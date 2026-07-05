package com.justnothing.richconsole.console;

/**
 * A context manager for capturing console output.
 * Ported from rich/console.py Capture.
 *
 * <p>Usage:</p>
 * <pre>
 * Capture capture = console.capture();
 * capture.start();
 * console.print("Hello");  // not displayed
 * capture.close();
 * String result = capture.get();
 * </pre>
 */
public class Capture implements AutoCloseable {

    private final Console console;
    private String result;

    /**
     * Create a new Capture.
     *
     * @param console the Console instance to capture output from
     */
    public Capture(Console console) {
        this.console = console;
        this.result = null;
    }

    /**
     * Begin capturing console output. Called automatically when used
     * in a try-with-resources block, but can also be called manually.
     */
    public Capture start() {
        console.beginCapture();
        return this;
    }

    /**
     * End capturing and store the result. Called automatically when used
     * in a try-with-resources block.
     */
    @Override
    public void close() {
        result = console.endCapture();
    }

    /**
     * Get the captured output.
     *
     * @return the captured string
     * @throws CaptureError if called before the capture context is closed
     */
    public String get() {
        if (result == null) {
            throw new CaptureError("Capture result is not available until context manager exits.");
        }
        return result;
    }
}