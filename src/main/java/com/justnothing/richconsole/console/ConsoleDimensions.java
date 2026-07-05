package com.justnothing.richconsole.console;

/**
 * Terminal dimensions record.
 * Ported from rich/console.py ConsoleDimensions named tuple.
 */
public record ConsoleDimensions(int width, int height) {

    @Override
    public String toString() {
        return "ConsoleDimensions(width=" + width + ", height=" + height + ")";
    }
}
