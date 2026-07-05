package com.justnothing.richconsole.color;

/**
 * Color utility methods ported from rich/color.py module-level functions.
 */
public final class ColorUtils {

    private ColorUtils() {}

    /**
     * Parse six hex characters into RGB triplet.
     */
    public static ColorTriplet parseRgbHex(String hexColor) {
        if (hexColor.length() != 6) {
            throw new IllegalArgumentException("must be 6 characters");
        }
        return new ColorTriplet(
            Integer.parseInt(hexColor.substring(0, 2), 16),
            Integer.parseInt(hexColor.substring(2, 4), 16),
            Integer.parseInt(hexColor.substring(4, 6), 16)
        );
    }

    /**
     * Blend one RGB color into another.
     *
     * @param color1    first color
     * @param color2    second color
     * @param crossFade blend factor (0.0 = color1, 1.0 = color2)
     * @return blended color
     */
    public static ColorTriplet blendRgb(ColorTriplet color1, ColorTriplet color2, float crossFade) {
        int r = (int) (color1.red() + (color2.red() - color1.red()) * crossFade);
        int g = (int) (color1.green() + (color2.green() - color1.green()) * crossFade);
        int b = (int) (color1.blue() + (color2.blue() - color1.blue()) * crossFade);
        return new ColorTriplet(r, g, b);
    }

    /**
     * Blend one RGB color into another with default cross-fade of 0.5.
     */
    public static ColorTriplet blendRgb(ColorTriplet color1, ColorTriplet color2) {
        return blendRgb(color1, color2, 0.5f);
    }
}
