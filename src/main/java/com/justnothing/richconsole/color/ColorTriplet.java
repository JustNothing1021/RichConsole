package com.justnothing.richconsole.color;

/**
 * The red, green, and blue components of a color.
 * Ported from rich/color_triplet.py.
 */
public record ColorTriplet(int red, int green, int blue) {

    /**
     * Get the color triplet in CSS hex style, e.g. "#ff0000".
     */
    public String hex() {
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    /**
     * The color in RGB format, e.g. "rgb(255,0,0)".
     */
    public String rgb() {
        return String.format("rgb(%d,%d,%d)", red, green, blue);
    }

    /**
     * Convert components into floats between 0 and 1.
     */
    public float[] normalized() {
        return new float[]{red / 255f, green / 255f, blue / 255f};
    }
}
