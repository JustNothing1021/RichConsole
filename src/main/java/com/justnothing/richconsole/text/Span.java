package com.justnothing.richconsole.text;

import com.justnothing.richconsole.style.Style;

/**
 * A span of text with an associated style.
 * Ported from rich/text.py Span NamedTuple.
 *
 * <p>A span defines a range [start, end) within a Text object and associates
 * it with a style. The style can be a String (style definition like "bold red")
 * or a {@link Style} object.</p>
 */
public record Span(int start, int end, Object style) {

    /**
     * Check if this span is valid (end > start).
     */
    public boolean isValid() {
        return end > start;
    }

    /**
     * Split this span at the given offset (relative to the span start).
     * Returns two spans: [start, start+offset) and [start+offset, end).
     *
     * @param offset the offset from the start of this span to split at
     * @return a two-element array: [left, right]
     */
    public Span[] split(int offset) {
        int splitPoint = start + offset;
        if (splitPoint <= start) {
            return new Span[]{new Span(start, start, style), this};
        }
        if (splitPoint >= end) {
            return new Span[]{this, new Span(end, end, style)};
        }
        return new Span[]{
            new Span(start, splitPoint, style),
            new Span(splitPoint, end, style)
        };
    }

    /**
     * Move this span by the given offset.
     *
     * @param offset the number of characters to move the span
     * @return a new Span shifted by offset
     */
    public Span move(int offset) {
        return new Span(start + offset, end + offset, style);
    }

    /**
     * Crop the right side of this span by reducing the end.
     *
     * @param offset the new end position (absolute)
     * @return a new Span with end set to the minimum of end and offset
     */
    public Span rightCrop(int offset) {
        return new Span(start, Math.min(end, offset), style);
    }

    /**
     * Extend this span by the given number of cells.
     * Increases the end by the given number.
     *
     * @param cells the number of cells to extend
     * @return a new Span with end increased by cells
     */
    public Span extend(int cells) {
        return new Span(start, end + cells, style);
    }

    @Override
    public String toString() {
        return "Span(" + start + ", " + end + ", " + style + ")";
    }
}
