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

    /**
     * Blend this color toward {@code other} by {@code crossFade} (0..1),
     * matching rich's {@code blend_rgb}. Used for dimmed SVG colors.
     */
    public ColorTriplet blend(ColorTriplet other, double crossFade) {
        int r = (int) ((other.red * crossFade) + (red * (1 - crossFade)));
        int g = (int) ((other.green * crossFade) + (green * (1 - crossFade)));
        int b = (int) ((other.blue * crossFade) + (blue * (1 - crossFade)));
        return new ColorTriplet(r, g, b);
    }
}
