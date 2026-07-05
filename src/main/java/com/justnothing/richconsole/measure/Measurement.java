package com.justnothing.richconsole.measure;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;

/**
 * Measurement record for renderable width ranges.
 * Ported from rich/measure.py Measurement named tuple.
 */
public record Measurement(int minimum, int maximum) {

    public Measurement {
        if (minimum < 0) {
            minimum = 0;
        }
        if (maximum < minimum) {
            maximum = minimum;
        }
    }

    /**
     * Get the span (difference between maximum and minimum).
     */
    public int span() {
        return maximum - minimum;
    }

    /**
     * Normalize the measurement so that minimum and maximum are &gt;= 1.
     */
    public Measurement normalize() {
        int min = Math.max(minimum, 1);
        int max = Math.max(maximum, 1);
        return new Measurement(min, max);
    }

    /**
     * Create a new measurement with the given maximum.
     */
    public Measurement withMaximum(int width) {
        int min = Math.min(minimum, width);
        return new Measurement(min, width);
    }

    /**
     * Create a new measurement with the given minimum.
     */
    public Measurement withMinimum(int width) {
        int max = Math.max(maximum, width);
        return new Measurement(width, max);
    }

    /**
     * Clamp the measurement to the given width range.
     *
     * @param minWidth minimum width (null to ignore)
     * @param maxWidth maximum width (null to ignore)
     * @return a clamped measurement
     */
    public Measurement clamp(Integer minWidth, Integer maxWidth) {
        int min = minimum;
        int max = maximum;
        if (minWidth != null) {
            min = Math.max(min, minWidth);
        }
        if (maxWidth != null) {
            max = Math.min(max, maxWidth);
        }
        return new Measurement(min, max);
    }

    /**
     * Get a measurement for a renderable object.
     * Ported from rich/measure.py Measurement.get().
     *
     * <p>If the renderable implements {@link RichRenderable}
     * and overrides {@code richMeasure()}, that method is used for the measurement.
     * Otherwise, a default measurement of {@code Measurement(0, maxWidth)} is returned.</p>
     *
     * @param console    the console instance
     * @param options    the console options
     * @param renderable the object to measure
     * @return a Measurement containing the width range
     */
    public static Measurement get(Console console, ConsoleOptions options, Object renderable) {
        int maxWidth = options.getMaxWidth();
        if (maxWidth < 1) {
            return new Measurement(0, 0);
        }

        // String: measure by rendering
        if (renderable instanceof String) {
            return new Measurement(((String) renderable).length(), maxWidth);
        }

        // RichRenderable with richMeasure
        if (renderable instanceof RichRenderable rr) {
            Measurement m = rr.richMeasure(console, options);
            return m.normalize().withMaximum(maxWidth);
        }

        // Default: the renderable can take up to the full width
        return new Measurement(0, maxWidth);
    }

    @Override
    public String toString() {
        return "Measurement(" + minimum + ", " + maximum + ")";
    }
}
