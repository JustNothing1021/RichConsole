package com.justnothing.richconsole.padding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Draw space around content.
 * Ported from rich/padding.py.
 */
public class Padding implements RichRenderable {

    private final Object renderable;
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;
    private final Object style;
    private final boolean expand;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Configuration class for creating Padding instances with optional parameters.
     */
    public static class Config {
        private int top = 0;
        private int right = 1;
        private int bottom = 0;
        private int left = 1;
        private Style style;
        private boolean expand = true;

        public Config top(int top) { this.top = top; return this; }
        public Config right(int right) { this.right = right; return this; }
        public Config bottom(int bottom) { this.bottom = bottom; return this; }
        public Config left(int left) { this.left = left; return this; }
        public Config style(Style style) { this.style = style; return this; }
        public Config expand(boolean expand) { this.expand = expand; return this; }

        /** Set uniform padding on all four sides. */
        public Config padding(int all) { this.top = all; this.right = all; this.bottom = all; this.left = all; return this; }

        /** Set vertical and horizontal padding. */
        public Config padding(int vertical, int horizontal) { this.top = vertical; this.right = horizontal; this.bottom = vertical; this.left = horizontal; return this; }

        /** Set individual padding for each side. */
        public Config padding(int top, int right, int bottom, int left) { this.top = top; this.right = right; this.bottom = bottom; this.left = left; return this; }
    }

    // =========================================================================
    // Static factory methods
    // =========================================================================

    /**
     * Create a Padding using a Configurer callback.
     *
     * @param renderable the content to pad
     * @param configurer a consumer that configures the Padding settings
     * @return a new Padding instance
     */
    public static Padding of(Object renderable, Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Padding(renderable, config);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private Padding(Object renderable, Config config) {
        this(renderable, config.top, config.right, config.bottom, config.left, config.style, config.expand);
    }

    /**
     * Create padding around a renderable.
     *
     * @param renderable the content to pad
     * @param top       top padding
     * @param right     right padding
     * @param bottom    bottom padding
     * @param left      left padding
     * @param style     optional style for padding characters
     * @param expand    whether to expand padding to fit available width
     */
    public Padding(Object renderable, int top, int right, int bottom, int left,
                   Style style, boolean expand) {
        this.renderable = renderable;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.style = style;
        this.expand = expand;
    }

    /**
     * Create padding with uniform padding on all sides.
     */
    public Padding(Object renderable, int pad) {
        this(renderable, pad, pad, pad, pad, null, true);
    }

    /**
     * Create padding with vertical and horizontal padding.
     */
    public Padding(Object renderable, int vertical, int horizontal) {
        this(renderable, vertical, horizontal, vertical, horizontal, null, true);
    }

    /**
     * Create padding with all four sides specified.
     */
    public Padding(Object renderable, int top, int right, int bottom, int left) {
        this(renderable, top, right, bottom, left, null, true);
    }

    /**
     * Factory: create padding with uniform amount on all sides.
     */
    public static Padding around(Object renderable, int amount) {
        return new Padding(renderable, amount, amount, amount, amount);
    }

    /**
     * Factory: create padding from a tuple-like int array (1, 2, or 4 elements).
     *
     * @param renderable the content
     * @param pad        int array of 1, 2, or 4 elements (CSS-style)
     * @return a new Padding instance
     */
    public static Padding fromTuple(Object renderable, int[] pad) {
        int[] unpacked = unpack(pad);
        return new Padding(renderable, unpacked[0], unpacked[1], unpacked[2], unpacked[3]);
    }

    /**
     * Make a padding instance to render an indent.
     *
     * @param renderable the content
     * @param level      number of characters to indent
     * @return a Padding instance
     */
    public static Padding indent(Object renderable, int level) {
        return new Padding(renderable, 0, 0, 0, level, null, false);
    }

    /**
     * Unpack padding specified in CSS style.
     *
     * @param pad int array of 1, 2, or 4 elements
     * @return a 4-element int array [top, right, bottom, left]
     */
    public static int[] unpack(int[] pad) {
        if (pad.length == 1) {
            int v = pad[0];
            return new int[]{v, v, v, v};
        }
        if (pad.length == 2) {
            return new int[]{pad[0], pad[1], pad[0], pad[1]};
        }
        if (pad.length == 4) {
            return new int[]{pad[0], pad[1], pad[2], pad[3]};
        }
        throw new IllegalArgumentException("1, 2 or 4 integers required for padding; " + pad.length + " given");
    }

    public Object getRenderable() {
        return renderable;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return right;
    }

    public int getBottom() {
        return bottom;
    }

    public int getLeft() {
        return left;
    }

    public Object getStyle() {
        return style;
    }

    public boolean isExpand() {
        return expand;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        Style styleResolved = style instanceof Style typedStyle ? typedStyle :
            (style != null ? console.getStyle(style.toString()) : Style.nullStyle());

        int maxWidth = options.getMaxWidth();
        int width;
        if (expand) {
            width = maxWidth;
        } else {
            Measurement measurement = console.measure(renderable, options);
            width = Math.min(measurement.maximum(), maxWidth);
        }

        int padRight = right;
        int padLeft = left;

        int childWidth = Math.max(0, width - padLeft - padRight);
        ConsoleOptions childOptions = options.updateWidth(childWidth);

        List<List<Segment>> lines = console.renderLines(renderable, childOptions, null, true, false);

        List<Segment> result = new ArrayList<>();
        Segment leftPad = new Segment(spaces(padLeft), styleResolved);
        Segment rightPad = new Segment(spaces(padRight), styleResolved);
        Segment blankLine = new Segment(spaces(width), styleResolved);

        // Top padding
        for (int i = 0; i < top; i++) {
            result.add(blankLine);
            result.add(Segment.line());
        }

        // Content lines with side padding
        for (List<Segment> line : lines) {
            result.add(leftPad);
            for (Segment seg : line) {
                result.add(seg);
            }
            result.add(rightPad);
            result.add(Segment.line());
        }

        // Bottom padding
        for (int i = 0; i < bottom; i++) {
            result.add(blankLine);
            result.add(Segment.line());
        }

        return result;
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        Measurement measurement = console.measure(renderable, options);
        int padWidth = left + right;
        return new Measurement(measurement.minimum() + padWidth, measurement.maximum() + padWidth);
    }

    private static String spaces(int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(' ');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Padding(" + renderable + ", (" + top + "," + right + "," + bottom + "," + left + "))";
    }
}
