package com.justnothing.richconsole.progress;

/**
 * Static formatting helpers shared by the progress columns.
 * Ported from rich/_filesize.py (filesize.decimal / pick_unit_and_suffix)
 * and the format helpers in rich/progress.py.
 */
public final class ProgressFormats {

    /** SI suffixes used by {@link #decimal(long)}. */
    private static final String[] DECIMAL_SUFFIXES =
            {"kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    /** SI suffixes used when rendering iteration speed (it/s). */
    private static final String[] SPEED_SUFFIXES =
            {"", "×10³", "×10⁶", "×10⁹", "×10¹²"};

    private ProgressFormats() {
    }

    /** A unit (multiplier) together with its display suffix. */
    public record Unit(long unit, String suffix) {
    }

    /**
     * Pick a unit and suffix for a size, mirroring filesize.pick_unit_and_suffix.
     *
     * @param size     the size in bytes
     * @param suffixes the ordered suffixes, starting from unit 1
     * @param base     the base (1000 for decimal, 1024 for binary)
     * @return the unit multiplier and matching suffix
     */
    public static Unit pickUnitAndSuffix(long size, String[] suffixes, int base) {
        long unit = 1;
        int i = 0;
        for (; i < suffixes.length; i++) {
            if (size < unit * base) {
                return new Unit(unit, suffixes[i]);
            }
            unit = unit * base;
        }
        // Size exceeds every suffix: clamp to the largest one.
        return new Unit(unit, suffixes[suffixes.length - 1]);
    }

    /**
     * Convert a file size to a string using SI (powers of 1000) prefixes.
     *
     * @param size the size in bytes
     * @return e.g. "30.0 kB", "1,234 bytes", "1 byte"
     */
    public static String decimal(long size) {
        return decimal(size, 1, " ");
    }

    /**
     * Convert a file size to a string using SI (powers of 1000) prefixes.
     *
     * @param size      the size in bytes
     * @param precision number of decimal places
     * @param separator string between value and unit
     * @return the formatted size
     */
    public static String decimal(long size, int precision, String separator) {
        if (size == 1) {
            return "1 byte";
        }
        if (size < 1000) {
            return String.format("%,d bytes", size);
        }
        Unit unit = pickUnitAndSuffix(size, DECIMAL_SUFFIXES, 1000);
        double value = (1000.0 * size) / unit.unit();
        return String.format("%," + precision + "f", value) + separator + unit.suffix();
    }

    /**
     * Format a duration in seconds as h:mm:ss (or -:--:-- for negatives).
     */
    public static String formatTime(double seconds) {
        if (seconds < 0) {
            return "-:--:--";
        }
        int totalSeconds = (int) seconds;
        int hrs = totalSeconds / 3600;
        int mins = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d:%02d", hrs, mins, secs);
    }

    /**
     * Format a byte transfer rate as e.g. "30.0 kB/s".
     */
    public static String formatSpeed(double speed) {
        return decimal((long) speed) + "/s";
    }

    /**
     * Format an iteration rate (items per second), e.g. "1.2×10³ it/s".
     */
    public static String formatIterationSpeed(double speed) {
        Unit unit = pickUnitAndSuffix((long) speed, SPEED_SUFFIXES, 1000);
        double value = speed / unit.unit();
        return String.format("%.1f%s it/s", value, unit.suffix());
    }

    /**
     * Render a number without a trailing ".0" when it is integral.
     */
    public static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value) && value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
            return String.format("%.0f", value);
        }
        return String.valueOf(value);
    }
}
