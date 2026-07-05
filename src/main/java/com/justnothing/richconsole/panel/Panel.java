package com.justnothing.richconsole.panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.cells.Cells;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.text.Span;
import com.justnothing.richconsole.text.Text;

/**
 * A panel with a box border and optional title/subtitle.
 * Ported from rich/panel.py Panel class.
 *
 * <p>Supports title_align and subtitle_align ("left", "center", "right")
 * to control the alignment of the title and subtitle within the border.</p>
 *
 * <p>Use {@link Config} for convenient construction:</p>
 * <pre>{@code
 * new Panel(renderable, cfg -> cfg.title("Hello").borderStyle("red").expand(false))
 * }</pre>
 */
public class Panel implements RichRenderable {

    private static final Box DEFAULT_BOX = Box.ROUNDED;
    private static final String DEFAULT_TITLE_ALIGN = "center";
    private static final String DEFAULT_SUBTITLE_ALIGN = "center";
    private final Object renderable;
    private final Object title;
    private final String titleAlign;
    private final Object subtitle;
    private final String subtitleAlign;
    private final Box box;
    private final boolean expand;
    private final Object style;
    private final Object borderStyle;
    private final Integer width;
    private final Integer height;
    private final int[] padding; // top, right, bottom, left

    // =========================================================================
    // Config — fluent configuration for Panel construction
    // =========================================================================

    /**
     * Fluent configuration object for Panel construction.
     * Usage: {@code new Panel(renderable, cfg -> cfg.title("Hello").borderStyle("red"))}
     */
    public static class Config {
        public Object title;
        public String titleAlign = DEFAULT_TITLE_ALIGN;
        public Object subtitle;
        public String subtitleAlign = DEFAULT_SUBTITLE_ALIGN;
        public Box box = DEFAULT_BOX;
        public boolean expand = true;
        public Object style;
        public Object borderStyle;
        public Integer width;
        public Integer height;
        public int[] padding = new int[]{0, 1, 0, 1};

        public Config title(Object title) { this.title = title; return this; }
        public Config titleAlign(String align) { this.titleAlign = align; return this; }
        public Config subtitle(Object subtitle) { this.subtitle = subtitle; return this; }
        public Config subtitleAlign(String align) { this.subtitleAlign = align; return this; }
        public Config box(Box box) { this.box = box; return this; }
        public Config expand(boolean expand) { this.expand = expand; return this; }
        public Config style(Object style) { this.style = style; return this; }
        public Config borderStyle(Object borderStyle) { this.borderStyle = borderStyle; return this; }
        public Config width(int width) { this.width = width; return this; }
        public Config height(int height) { this.height = height; return this; }
        public Config padding(int top, int right, int bottom, int left) {
            this.padding = new int[]{top, right, bottom, left}; return this;
        }
        public Config padding(int horizontal, int vertical) {
            this.padding = new int[]{vertical, horizontal, vertical, horizontal}; return this;
        }
        public Config padding(int all) {
            this.padding = new int[]{all, all, all, all}; return this;
        }
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Full constructor with all parameters.
     */
    public Panel(Object renderable, Object title, String titleAlign,
                 Object subtitle, String subtitleAlign, Box box,
                 boolean expand, Object style, Object borderStyle,
                 Integer width, Integer height, int[] padding) {
        this.renderable = renderable;
        this.title = title;
        this.titleAlign = titleAlign != null ? titleAlign : DEFAULT_TITLE_ALIGN;
        this.subtitle = subtitle;
        this.subtitleAlign = subtitleAlign != null ? subtitleAlign : DEFAULT_SUBTITLE_ALIGN;
        this.box = box != null ? box : DEFAULT_BOX;
        this.expand = expand;
        this.style = style;
        this.borderStyle = borderStyle;
        this.width = width;
        this.height = height;
        this.padding = padding != null ? padding : new int[]{0, 1, 0, 1};
    }

    /**
     * Construct a Panel with a Config consumer for fluent configuration.
     * <pre>{@code
     * new Panel(renderable, cfg -> cfg.title("Hello").borderStyle("red").expand(false))
     * }</pre>
     */
    public Panel(Object renderable, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        this.renderable = renderable;
        this.title = cfg.title;
        this.titleAlign = cfg.titleAlign;
        this.subtitle = cfg.subtitle;
        this.subtitleAlign = cfg.subtitleAlign;
        this.box = cfg.box;
        this.expand = cfg.expand;
        this.style = cfg.style;
        this.borderStyle = cfg.borderStyle;
        this.width = cfg.width;
        this.height = cfg.height;
        this.padding = cfg.padding;
    }

    /**
     * Construct a Panel with a title and Config consumer.
     * <pre>{@code
     * new Panel(renderable, "Title", cfg -> cfg.borderStyle("red"))
     * }</pre>
     */
    public Panel(Object renderable, Object title, Consumer<Config> configurer) {
        Config cfg = new Config();
        cfg.title = title;
        configurer.accept(cfg);
        this.renderable = renderable;
        this.title = cfg.title;
        this.titleAlign = cfg.titleAlign;
        this.subtitle = cfg.subtitle;
        this.subtitleAlign = cfg.subtitleAlign;
        this.box = cfg.box;
        this.expand = cfg.expand;
        this.style = cfg.style;
        this.borderStyle = cfg.borderStyle;
        this.width = cfg.width;
        this.height = cfg.height;
        this.padding = cfg.padding;
    }

    public Panel(Object renderable, Object title, Object subtitle, Box box,
                 boolean expand, Object style, Object borderStyle,
                 Integer width, Integer height, int[] padding) {
        this(renderable, title, null, subtitle, null, box, expand, style,
                borderStyle, width, height, padding);
    }

    public Panel(Object renderable) {
        this(renderable, null, null, null, null, DEFAULT_BOX, true, null, null, null, null, null);
    }

    public Panel(Object renderable, Object title) {
        this(renderable, title, null, null, null, DEFAULT_BOX, true, null, null, null, null, null);
    }

    public Panel(Object renderable, Object title, Object subtitle) {
        this(renderable, title, null, subtitle, null, DEFAULT_BOX, true, null, null, null, null, null);
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Create a Panel with a Config consumer for fluent configuration.
     * <pre>{@code
     * Panel.of(renderable, cfg -> cfg.title("Hello").borderStyle("red"))
     * }</pre>
     */
    public static Panel of(Object renderable, Consumer<Config> configurer) {
        return new Panel(renderable, configurer);
    }

    /**
     * Create a Panel with a title and Config consumer.
     */
    public static Panel of(Object renderable, Object title, Consumer<Config> configurer) {
        return new Panel(renderable, title, configurer);
    }

    /**
     * Create a Panel that fits its content width (expand=false).
     * Matches Python rich's Panel.fit() classmethod.
     */
    public static Panel fit(Object renderable, Consumer<Config> configurer) {
        Config cfg = new Config();
        cfg.expand = false;
        configurer.accept(cfg);
        // Build Panel directly from Config fields (not via constructor which may mis-resolve)
        return new Panel(renderable, cfg.title, cfg.titleAlign, cfg.subtitle,
                cfg.subtitleAlign, cfg.box, cfg.expand, cfg.style, cfg.borderStyle,
                cfg.width, cfg.height, cfg.padding);
    }

    /**
     * Create a Panel that fits its content width (expand=false).
     */
    public static Panel fit(Object renderable) {
        return new Panel(renderable, cfg -> cfg.expand(false));
    }

    /**
     * Create a Panel that fits its content width with a title.
     */
    public static Panel fit(Object renderable, Object title) {
        return new Panel(renderable, title, cfg -> cfg.expand(false));
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        Style borderStyleResolved = resolveBorderStyle(console);
        Style styleResolved = resolveStyle(console);

        int maxWidth = options.getMaxWidth();
        int panelWidth = resolvePanelWidth(console, options, maxWidth);
        int innerWidth = Math.max(0, panelWidth - 2);

        // Account for padding in inner width
        int padLeft = padding[3];
        int padRight = padding[1];
        int contentWidth = Math.max(0, innerWidth - padLeft - padRight);

        // Render the inner content (without height constraint - Panel sizes to its content)
        ConsoleOptions innerOptions = options.updateWidth(contentWidth).clearHeight();
        List<List<Segment>> contentLines = console.renderLines(renderable, innerOptions, null, true, false);

        // Apply vertical padding
        int padTop = padding[0];
        int padBottom = padding[2];
        List<List<Segment>> paddedLines = new ArrayList<>();
        Segment padSegment = new Segment(spaces(contentWidth), styleResolved);
        for (int i = 0; i < padTop; i++) {
            paddedLines.add(Collections.singletonList(padSegment));
        }
        paddedLines.addAll(contentLines);
        for (int i = 0; i < padBottom; i++) {
            paddedLines.add(Collections.singletonList(padSegment));
        }

        // Apply height constraint
        if (height != null) {
            int targetLines = Math.max(0, height - 2);
            while (paddedLines.size() < targetLines) {
                paddedLines.add(Collections.singletonList(padSegment));
            }
            if (paddedLines.size() > targetLines) {
                paddedLines = paddedLines.subList(0, targetLines);
            }
        }

        // Build the output
        List<Segment> result = new ArrayList<>();

        // Top border with optional title
        result.addAll(buildBorderLine(console, box.topLeft, box.top, box.topRight,
                innerWidth, title, titleAlign, borderStyleResolved, styleResolved));

        // Content lines with side borders and padding
        Segment leftBorder = new Segment(box.midVertical != null ? box.midVertical : box.midLeft, borderStyleResolved);
        Segment rightBorder = new Segment(box.midVertical != null ? box.midVertical : box.midRight, borderStyleResolved);
        Segment leftPad = new Segment(spaces(padLeft), styleResolved);
        Segment rightPad = new Segment(spaces(padRight), styleResolved);

        for (List<Segment> line : paddedLines) {
            result.add(leftBorder);
            result.add(leftPad);
            List<Segment> adjustedLine = Segment.adjustLineLength(line, contentWidth, styleResolved, true);
            for (Segment seg : adjustedLine) result.add(seg);
            result.add(rightPad);
            result.add(rightBorder);
            result.add(Segment.line());
        }

        // Bottom border with optional subtitle
        result.addAll(buildBorderLine(console, box.bottomLeft, box.bottom, box.bottomRight,
                innerWidth, subtitle, subtitleAlign, borderStyleResolved, styleResolved));

        return result;
    }

    private Style resolveBorderStyle(Console console) {
        Style bs;
        if (borderStyle instanceof Style) {
            bs = (Style) borderStyle;
        } else {
            bs = console.getStyle(borderStyle != null ? borderStyle.toString() : "");
        }
        // Apply panel style on top of border style (matching Python rich)
        Style panelStyle = resolveStyle(console);
        if (!panelStyle.isNull()) {
            bs = panelStyle.add(bs);
        }
        return bs;
    }

    private Style resolveStyle(Console console) {
        if (style instanceof Style) {
            return (Style) style;
        }
        if (style != null && !style.toString().isEmpty()) {
            try {
                return console.getStyle(style);
            } catch (Exception e) {
                return Style.nullStyle();
            }
        }
        return Style.nullStyle();
    }

    private int resolvePanelWidth(Console console, ConsoleOptions options, int maxWidth) {
        if (width != null) {
            return expand ? Math.min(width, maxWidth) : width;
        }
        if (expand) {
            return maxWidth;
        }
        // When not expanding, measure the content to determine width
        Measurement measurement = console.measure(renderable, options);
        int contentWidth = measurement.maximum();
        // Add border overhead (left + right borders = 2)
        int panelWidth = contentWidth + 2;
        // Also consider padding
        panelWidth += padding[3] + padding[1]; // left + right padding
        return Math.min(panelWidth, maxWidth);
    }

    /**
     * Build a border line (top or bottom) with an optional title/subtitle embedded.
     * Supports left/center/right alignment, matching rich's align_text() logic.
     */
    // TODO: use console.render() for title rendering with proper style resolution (Python uses console.render(title_text, child_options))
    private List<Segment> buildBorderLine(Console console, String left, String horizontal,
                                          String right, int innerWidth, Object titleObj,
                                          String align, Style borderStyle, Style textStyle) {
        List<Segment> segments = new ArrayList<>();
        segments.add(new Segment(left, borderStyle));

        if (titleObj == null || innerWidth <= 4) {
            // No title — simple border line
            segments.add(new Segment(repeat(horizontal, innerWidth), borderStyle));
        } else {
            String titleText = resolveText(titleObj);
            if (titleText.isEmpty()) {
                segments.add(new Segment(repeat(horizontal, innerWidth), borderStyle));
            } else {
                String paddedTitle = " " + titleText + " ";
                int titleCellLen = Cells.cellLen(paddedTitle);
                int borderCellLen = Math.max(0, innerWidth - titleCellLen);

                int leftBorderLen;
                int rightBorderLen;

                if ("left".equals(align)) {
                    leftBorderLen = 0;
                    rightBorderLen = borderCellLen;
                } else if ("right".equals(align)) {
                    leftBorderLen = borderCellLen;
                    rightBorderLen = 0;
                } else {
                    // center (default)
                    leftBorderLen = borderCellLen / 2;
                    rightBorderLen = borderCellLen - leftBorderLen;
                }

                // Left border portion
                if (leftBorderLen > 0) {
                    segments.add(new Segment(repeat(horizontal, leftBorderLen), borderStyle));
                }
                // Title portion
                Text titleRenderable;
                if (titleObj instanceof Text) {
                    // Copy and add padding spaces around the title
                    Text original = ((Text) titleObj).copy();
                    titleRenderable = new Text();
                    titleRenderable.append(" ", borderStyle);
                    // Copy all spans from original, adjusted for the leading space
                    for (Span span : original.getSpans()) {
                        titleRenderable.stylize(span.style(), span.start() + 1, span.end() + 1);
                    }
                    titleRenderable.append(original.getPlain(), borderStyle);
                    titleRenderable.append(" ", borderStyle);
                    titleRenderable.stylizeBefore(borderStyle, 0, titleRenderable.length());
                } else {
                    titleRenderable = Text.styled(paddedTitle, borderStyle);
                }
                titleRenderable.setEnd("");
                titleRenderable.setNoWrap(true);
                Iterable<Segment> titleSegments = titleRenderable.render(null, "");
                for (Segment seg : titleSegments) {
                    if (!"\n".equals(seg.getText())) {
                        segments.add(seg);
                    }
                }
                // Right border portion
                if (rightBorderLen > 0) {
                    segments.add(new Segment(repeat(horizontal, rightBorderLen), borderStyle));
                }
            }
        }

        segments.add(new Segment(right, borderStyle));
        segments.add(Segment.line());
        return segments;
    }

    private String resolveText(Object obj) {
        if (obj == null) return "";
        if (obj instanceof Text) return ((Text) obj).getPlain();
        String s = obj.toString();
        return s != null ? s : "";
    }

    private static String repeat(String s, int count) {
        if (s == null || s.isEmpty() || count <= 0) return "";
        StringBuilder sb = new StringBuilder(count * s.length());
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static String spaces(int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Panel(" + renderable + ")";
    }
}
