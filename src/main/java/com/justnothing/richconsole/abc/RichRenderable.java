package com.justnothing.richconsole.abc;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;

/**
 * The core renderable interface for Rich console output.
 * Ported from rich/abc.py RichRenderable.
 *
 * <p>Implementations must provide {@link #richConsole}, and may optionally
 * override {@link #richMeasure} to provide accurate width measurements
 * (equivalent to Python rich's {@code __rich_measure__} protocol).</p>
 */
public interface RichRenderable {

    /**
     * The rich console rendering method.
     *
     * @param console the console to render to
     * @param options the console options
     * @return an iterable of objects (Segment, RichRenderable, String, etc.)
     */
    Iterable<?> richConsole(Console console, ConsoleOptions options);

    /**
     * Measure the minimum and maximum width required to render this object.
     * Equivalent to Python rich's {@code __rich_measure__} protocol.
     *
     * <p>By default, returns {@code Measurement(0, maxWidth)} which means
     * the renderable can fit in any width from 0 to the full console width.
     * Implementations should override this to provide more precise measurements
     * (e.g., a Table can report its actual column widths).</p>
     *
     * @param console the console instance
     * @param options the console options
     * @return a Measurement with minimum and maximum widths
     */
    default Measurement richMeasure(Console console, ConsoleOptions options) {
        int maxWidth = options.getMaxWidth();
        if (maxWidth < 1) {
            return new Measurement(0, 0);
        }
        return new Measurement(0, maxWidth);
    }
}
