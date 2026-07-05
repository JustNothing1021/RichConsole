package com.justnothing.richconsole.status;

import java.util.function.Consumer;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.spinner.Spinner;

/**
 * A status indicator with a spinner animation.
 * Ported from rich/status.py Status class.
 *
 * <p>Status displays a spinner animation alongside a status message,
 * providing visual feedback during long-running operations. When the
 * status is stopped, the animation is automatically cleared from the
 * terminal (transient mode).</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (Status status = console.status("Loading...")) {
 *     // do work
 *     status.update("Processing...");
 * }
 * }</pre>
 */
public class Status implements AutoCloseable {

    private static final String DEFAULT_SPINNER_NAME = "dots";
    private static final String DEFAULT_SPINNER_STYLE = "status.spinner";
    private static final double DEFAULT_SPINNER_SPEED = 1.0;
    private static final double DEFAULT_REFRESH_PER_SECOND = 12.5;

    private Spinner spinner;
    private final Live live;

    // =========================================================================
    // Config inner class
    // =========================================================================

    public static class Config {
        public String spinnerName = "dots";
        public Object spinnerStyle = "status.spinner";
        public double speed = 1.0;
        public double refreshPerSecond = 12.5;
    }

    public static Status of(Object status, Console console, Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Status(status, console, config);
    }

    private Status(Object status, Console console, Config config) {
        this(status, console, config.spinnerName, config.spinnerStyle, config.speed, config.refreshPerSecond);
    }

    /**
     * Create a Status with full configuration.
     *
     * @param status            the status text (supports Rich markup)
     * @param console           the Console instance to use
     * @param spinnerName       the spinner animation name (e.g. "dots", "line", "earth")
     * @param spinnerStyle      the style for the spinner (e.g. "status.spinner")
     * @param speed             animation speed multiplier
     * @param refreshPerSecond  how many times per second to refresh
     */
    public Status(Object status, Console console, String spinnerName,
                  Object spinnerStyle, double speed, double refreshPerSecond) {
        this.spinner = new Spinner(spinnerName, status, spinnerStyle, speed);
        this.live = new Live(
                this.spinner, console, refreshPerSecond,
                true,  // transient — clear on stop
                true   // autoRefresh
        );
    }

    /**
     * Create a Status with default settings.
     *
     * @param status  the status text
     * @param console the Console instance to use
     */
    public Status(Object status, Console console) {
        this(status, console, DEFAULT_SPINNER_NAME, DEFAULT_SPINNER_STYLE, DEFAULT_SPINNER_SPEED, DEFAULT_REFRESH_PER_SECOND);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Start the status animation.
     */
    public void start() {
        live.start();
    }

    /**
     * Stop the status animation. The spinner output will be cleared.
     */
    public void stop() {
        live.stop();
    }

    /**
     * AutoCloseable support — stops the status on try-with-resources exit.
     * The status is automatically started when created via Console.status().
     */
    @Override
    public void close() {
        stop();
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Update the status display.
     *
     * <p>If {@code spinnerName} is provided (non-null and different from current),
     * the entire Spinner is rebuilt with the new animation type.
     * Otherwise, the existing Spinner's properties are updated in-place.</p>
     *
     * @param status       new status text (null for no change)
     * @param spinnerName  new spinner animation name (null for no change)
     * @param spinnerStyle new spinner style (null for no change)
     * @param speed        new speed (null for no change)
     */
    public void update(Object status, String spinnerName, Object spinnerStyle, Double speed) {
        if (spinnerName != null && !spinnerName.equals(spinner.getName())) {
            // Rebuild spinner with new type
            Object newStatus = status != null ? status : spinner.getText();
            Object newStyle = spinnerStyle != null ? spinnerStyle : spinner.getStyle();
            double newSpeed = speed != null ? speed : spinner.getSpeed();
            this.spinner = new Spinner(spinnerName, newStatus, newStyle, newSpeed);
            live.update(this.spinner, true);
        } else {
            // Update existing spinner
            spinner.update(status, spinnerStyle, speed);
        }
    }

    /**
     * Update only the status text.
     *
     * @param status new status text
     */
    public void update(Object status) {
        update(status, null, null, null);
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /**
     * Get the underlying Spinner object.
     */
    public Spinner getSpinner() {
        return spinner;
    }

    /**
     * Get the underlying Live instance.
     */
    public Live getLive() {
        return live;
    }

    /**
     * Get the Console used by this Status.
     */
    public Console getConsole() {
        return live.getConsole();
    }
}
