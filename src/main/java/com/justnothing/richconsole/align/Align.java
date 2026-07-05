package com.justnothing.richconsole.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.constrain.Constrain;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Align a renderable by adding spaces if necessary.
 * Ported from rich/align.py.
 */
public class Align implements RichRenderable {
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String CENTER = "center";
    public static final String TOP = "top";
    public static final String MIDDLE = "middle";
    public static final String BOTTOM = "bottom";

    private final Object renderable;
    private final String align;
    private final String vertical;
    private final boolean pad;
    private final Integer width;
    private final Integer height;
    private final Style style;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Configuration class for creating Align instances with optional parameters.
     */
    public static class Config {
        private String align = "left";
        private Style style;
        private String vertical = "top";
        private boolean pad = false;
        private Integer width;
        private Integer height;

        public Config align(String align) { this.align = align; return this; }
        public Config style(Style style) { this.style = style; return this; }
        public Config vertical(String vertical) { this.vertical = vertical; return this; }
        public Config pad(boolean pad) { this.pad = pad; return this; }
        public Config width(Integer width) { this.width = width; return this; }
        public Config height(Integer height) { this.height = height; return this; }
    }

    // =========================================================================
    // Static factory methods
    // =========================================================================

    /**
     * Create an Align using a Configurer callback.
     *
     * @param renderable the thing to align
     * @param configurer a consumer that configures the Align settings
     * @return a new Align instance
     */
    public static Align of(Object renderable, Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Align(renderable, config);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private Align(Object renderable, Config config) {
        this(renderable, config.align, config.style, config.vertical, config.pad, config.width, config.height);
    }

    /**
     * Create an Align instance.
     *
     * @param renderable the thing to align
     * @param align      one of "left", "center", or "right"
     * @param style      optional style for the background
     * @param vertical   optional vertical alignment: "top", "middle", or "bottom"
     * @param pad        whether to pad the right with spaces
     * @param width      optional width constraint
     * @param height     optional height constraint
     */
    public Align(Object renderable, String align, Style style,
                 String vertical, boolean pad, Integer width, Integer height) {
        if (!LEFT.equals(align) && !RIGHT.equals(align) && !CENTER.equals(align)) {
            throw new IllegalArgumentException(
                "invalid value for align, expected one of " + Arrays.toString(new String[]{LEFT, CENTER, RIGHT}) + " (not \"" + align + "\")");
        }
        if (vertical != null && !TOP.equals(vertical) && !MIDDLE.equals(vertical) && !BOTTOM.equals(vertical)) {
            throw new IllegalArgumentException(
                "invalid value for vertical, expected one of " + Arrays.toString(new String[]{TOP, MIDDLE, BOTTOM}) + " (not \"" + vertical + "\")");
        }
        this.renderable = renderable;
        this.align = align;
        this.style = style;
        this.vertical = vertical;
        this.pad = pad;
        this.width = width;
        this.height = height;
    }

    /**
     * Create an Align with sensible defaults.
     */
    public Align(Object renderable, String align, Integer width, Integer height, Style style) {
        this(renderable, align, style, null, true, width, height);
    }

    /**
     * Align a renderable to the left.
     */
    public static Align left(Object renderable, Integer width, Style style) {
        return new Align(renderable, LEFT, style, null, true, width, null);
    }

    /**
     * Align a renderable to the center.
     */
    public static Align center(Object renderable, Integer width, Style style) {
        return new Align(renderable, CENTER, style, null, true, width, null);
    }

    /**
     * Align a renderable to the right.
     */
    public static Align right(Object renderable, Integer width, Style style) {
        return new Align(renderable, RIGHT, style, null, true, width, null);
    }

    public Object getRenderable() {
        return renderable;
    }

    public String getAlign() {
        return align;
    }

    public String getVertical() {
        return vertical;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Style getStyle() {
        return style;
    }

    public boolean isPad() {
        return pad;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        // Measure the renderable to determine its natural maximum width
        Measurement measurement = console.measure(renderable, options);
        int measuredWidth = measurement.maximum();

        // Determine the constraint width: if this.width is set, use min(measured, this.width)
        int constrainWidth = this.width != null ? Math.min(measuredWidth, this.width) : measuredWidth;

        // Render the renderable with a Constrain wrapper and no height limit
        Constrain constrained = new Constrain(renderable, constrainWidth);
        ConsoleOptions renderOptions = options.resetHeight();
        Iterable<Segment> rendered = console.render(constrained, renderOptions);

        // Split rendered segments into lines
        List<List<Segment>> lines = new ArrayList<>();
        for (List<Segment> line : Segment.splitLines(rendered)) {
            lines.add(line);
        }

        // Get shape and set shape (uniform width/height)
        int[] shape = Segment.getShape(lines);
        int contentWidth = shape[0];
        int contentHeight = shape[1];
        lines = Segment.setShape(lines, contentWidth, contentHeight, null, false);

        Segment newLine = Segment.line();
        int excessSpace = options.getMaxWidth() - contentWidth;
        Style resolvedStyle = style != null ? console.getStyle(style) : null;

        // Generate horizontally-aligned segments
        List<Segment> segments = new ArrayList<>();

        if (excessSpace <= 0) {
            // Exact fit or content wider than available space
            for (List<Segment> line : lines) {
                segments.addAll(line);
                segments.add(newLine);
            }
        } else if (LEFT.equals(align)) {
            Segment padSeg = pad ? new Segment(spaces(excessSpace), resolvedStyle) : null;
            for (List<Segment> line : lines) {
                segments.addAll(line);
                if (padSeg != null) {
                    segments.add(padSeg);
                }
                segments.add(newLine);
            }
        } else if (CENTER.equals(align)) {
            int left = excessSpace / 2;
            Segment padLeft = new Segment(spaces(left), resolvedStyle);
            Segment padRight = pad ? new Segment(spaces(excessSpace - left), resolvedStyle) : null;
            for (List<Segment> line : lines) {
                if (left > 0) {
                    segments.add(padLeft);
                }
                segments.addAll(line);
                if (padRight != null) {
                    segments.add(padRight);
                }
                segments.add(newLine);
            }
        } else { // RIGHT
            Segment padSeg = new Segment(spaces(excessSpace), resolvedStyle);
            for (List<Segment> line : lines) {
                segments.add(padSeg);
                segments.addAll(line);
                segments.add(newLine);
            }
        }

        // Vertical alignment
        Integer verticalHeight = this.height != null ? this.height : options.getHeight();
        if (vertical != null && verticalHeight != null) {
            int topSpace = 0;
            int bottomSpace = 0;

            if (TOP.equals(vertical)) {
                bottomSpace = verticalHeight - contentHeight;
            } else if (MIDDLE.equals(vertical)) {
                topSpace = (verticalHeight - contentHeight) / 2;
                bottomSpace = verticalHeight - topSpace - contentHeight;
            } else { // BOTTOM
                topSpace = verticalHeight - contentHeight;
            }

            int blankLineWidth = this.width != null ? this.width : options.getMaxWidth();
            Segment blankLine = pad
                    ? new Segment(spaces(blankLineWidth) + "\n", resolvedStyle)
                    : new Segment("\n");

            List<Segment> result = new ArrayList<>();
            for (int i = 0; i < topSpace; i++) {
                result.add(blankLine);
            }
            result.addAll(segments);
            for (int i = 0; i < bottomSpace; i++) {
                result.add(blankLine);
            }
            segments = result;
        }

        // Apply style to all segments if specified
        if (style != null) {
            Style resolved = console.getStyle(style);
            List<Segment> styled = new ArrayList<>();
            for (Segment seg : Segment.applyStyle(segments, resolved, null)) {
                styled.add(seg);
            }
            segments = styled;
        }

        return segments;
    }

    private static String spaces(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Align(" + renderable + ", " + align + ")";
    }
}
