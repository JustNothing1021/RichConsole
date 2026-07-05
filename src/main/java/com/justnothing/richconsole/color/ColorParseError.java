package com.justnothing.richconsole.color;

/**
 * The color could not be parsed.
 * Ported from rich/color.py ColorParseError.
 */
public class ColorParseError extends RuntimeException {
    public ColorParseError(String message) {
        super(message);
    }
}
