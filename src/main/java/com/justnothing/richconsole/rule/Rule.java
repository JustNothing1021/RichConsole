package com.justnothing.richconsole.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.align.Align;
import com.justnothing.richconsole.cells.Cells;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.text.Text;

/**
 * A horizontal rule with an optional title.
 * Ported from rich/rule.py Rule class.
 */
public class Rule implements RichRenderable {

    private static final String DEFAULT_CHARACTERS = "─";
    private static final String DEFAULT_STYLE = "rule.line";
    private static final String DEFAULT_ALIGN = "center";
    private static final String ASCII_RULE_CHARACTER = "-";

    private final Object title;
    private final String characters;
    private final Object style;
    private final String align;

    // =========================================================================
    // Config — fluent configuration for Rule construction
    // =========================================================================

    /**
     * Fluent configuration object for Rule construction.
     * Usage: {@code new Rule("Title", cfg -> cfg.style("red").align("left"))}
     */
    public static class Config {
        public String characters = DEFAULT_CHARACTERS;
        public Object style = DEFAULT_STYLE;
        public String align = DEFAULT_ALIGN;

        public Config characters(String characters) { this.characters = characters; return this; }
        public Config style(Object style) { this.style = style; return this; }
        public Config align(String align) { this.align = align; return this; }
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    public Rule(Object title, String characters, Object style, String align) {
        if (characters != null && Cells.cellLen(characters) < 1) {
            throw new IllegalArgumentException("'characters' argument must have a cell width of at least 1");
        }
        if (align != null && !Align.LEFT.equals(align) && !Align.CENTER.equals(align) && !Align.RIGHT.equals(align)) {
            throw new IllegalArgumentException("invalid value for align, must be one of " + Arrays.toString(new Object[]{Align.LEFT, Align.CENTER, Align.RIGHT}));
        }
        this.title = title;
        this.characters = characters != null && !characters.isEmpty() ? characters : DEFAULT_CHARACTERS;
        this.style = style;
        this.align = align != null ? align : DEFAULT_ALIGN;
    }

    public Rule(Object title) {
        this(title, DEFAULT_CHARACTERS, DEFAULT_STYLE, DEFAULT_ALIGN);
    }

    public Rule() {
        this("");
    }

    /**
     * Construct a Rule with a Config consumer for fluent configuration.
     * <pre>{@code
     * new Rule("Title", cfg -> cfg.style("red").align("left"))
     * }</pre>
     */
    public Rule(Object title, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        this.title = title;
        this.characters = cfg.characters;
        this.style = cfg.style;
        this.align = cfg.align;
        if (Cells.cellLen(this.characters) < 1) {
            throw new IllegalArgumentException("'characters' argument must have a cell width of at least 1");
        }
        if (!Align.LEFT.equals(this.align) && !Align.CENTER.equals(this.align) && !Align.RIGHT.equals(this.align)) {
            throw new IllegalArgumentException("invalid value for align, must be one of " + Arrays.toString(new Object[]{Align.LEFT, Align.CENTER, Align.RIGHT}));
        }
    }

    /**
     * Create a Rule with a Config consumer for fluent configuration.
     * <pre>{@code
     * Rule.of("Title", cfg -> cfg.style("red").align("left"))
     * }</pre>
     */
    public static Rule of(Object title, Consumer<Config> configurer) {
        return new Rule(title, configurer);
    }

    /**
     * Create a Rule with default settings.
     */
    public static Rule of() {
        return new Rule();
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        Style ruleStyle = console.getStyle(style != null ? style : DEFAULT_STYLE);
        int width = options.getMaxWidth();

        // Use ASCII fallback if needed
        String chars = characters;
        if (options.isAsciiOnly() && !isAscii(chars)) {
            chars = ASCII_RULE_CHARACTER;
        }

        // Resolve title to a Text object, applying "rule.text" style for String titles
        Text titleTextObj = null;
        if (title instanceof Text) {
            titleTextObj = ((Text) title).copy();
        } else if (title instanceof String) {
            // Apply "rule.text" style from theme (not parsed as inline style)
            Style ruleTextStyle;
            try {
                ruleTextStyle = console.getStyle("rule.text");
            } catch (Exception e) {
                ruleTextStyle = ruleStyle; // fallback to rule's own style
            }
            titleTextObj = new Text((String) title, ruleTextStyle);
            titleTextObj.setPlain(titleTextObj.getPlain().replace("\n", " "));
            int maxTitleWidth = width - 4;
            if (maxTitleWidth > 0 && Cells.cellLen(titleTextObj.getPlain()) > maxTitleWidth) {
                titleTextObj.truncate(maxTitleWidth, "ellipsis", false);
            }
            titleTextObj.setEnd("");
        }

        String titleText = titleTextObj != null ? titleTextObj.getPlain() : "";
        if (titleText.isEmpty()) {
            // No title — just a full-width rule line
            String ruleLine = setCellSize(repeatToWidth(chars, width), width);
            List<Segment> result = new ArrayList<>();
            result.add(new Segment(ruleLine, ruleStyle));
            result.add(Segment.line());
            return result;
        }

        // Build " title " string
        String paddedTitle = " " + titleText + " ";
        int titleCellLen = Cells.cellLen(paddedTitle);

        // If title is too wide, fall back to rule-only line
        int requiredSpace = Align.CENTER.equals(align) ? 4 : 2;
        int truncateWidth = Math.max(0, width - requiredSpace);
        if (truncateWidth == 0) {
            String ruleLine = setCellSize(repeatToWidth(chars, width), width);
            List<Segment> result = new ArrayList<>();
            result.add(new Segment(ruleLine, ruleStyle));
            result.add(Segment.line());
            return result;
        }

        List<Segment> result = new ArrayList<>();

        if (Align.CENTER.equals(align)) {
            int sideWidth = (width - titleCellLen) / 2;
            String left = setCellSize(repeatToWidth(chars, sideWidth - 1), sideWidth - 1);
            int rightLength = width - Cells.cellLen(left) - titleCellLen;
            String right = setCellSize(repeatToWidth(chars, rightLength), rightLength);

            result.add(new Segment(left + " ", ruleStyle));
            // Title part
            Text titleRenderable;
            if (title instanceof Text) {
                titleRenderable = titleTextObj;
                titleRenderable.stylizeBefore(ruleStyle, 0, titleRenderable.length());
            } else {
                titleRenderable = titleTextObj;
            }
            Iterable<Segment> titleSegments = titleRenderable.render(console, "");
            for (Segment seg : titleSegments) {
                if (!"\n".equals(seg.getText())) {
                    result.add(seg);
                }
            }
            result.add(new Segment(" " + right, ruleStyle));
        } else if (Align.LEFT.equals(align)) {
            // Left-aligned: rule chars + space + title + space + rule chars
            result.add(new Segment(chars, ruleStyle));
            result.add(new Segment(" ", ruleStyle));
            // Title part
            Text titleRenderable;
            if (title instanceof Text) {
                titleRenderable = titleTextObj;
                titleRenderable.stylizeBefore(ruleStyle, 0, titleRenderable.length());
            } else {
                titleRenderable = titleTextObj;
            }
            Iterable<Segment> titleSegments = titleRenderable.render(console, "");
            for (Segment seg : titleSegments) {
                if (!"\n".equals(seg.getText())) {
                    result.add(seg);
                }
            }
            result.add(new Segment(" ", ruleStyle));
            int remainingWidth = width - Cells.cellLen(titleText) - 3; // " " + title + " "
            if (remainingWidth > 0) {
                String right = setCellSize(repeatToWidth(chars, remainingWidth), remainingWidth);
                result.add(new Segment(right, ruleStyle));
            }
        } else {
            // Right-aligned: rule chars + space + title + space + rule chars
            int remainingWidth = width - Cells.cellLen(titleText) - 3; // " " + title + " "
            if (remainingWidth > 0) {
                String left = setCellSize(repeatToWidth(chars, remainingWidth), remainingWidth);
                result.add(new Segment(left, ruleStyle));
            }
            result.add(new Segment(" ", ruleStyle));
            Text titleRenderable;
            if (title instanceof Text) {
                titleRenderable = titleTextObj;
                titleRenderable.stylizeBefore(ruleStyle, 0, titleRenderable.length());
            } else {
                titleRenderable = titleTextObj;
            }
            Iterable<Segment> titleSegments = titleRenderable.render(console, "");
            for (Segment seg : titleSegments) {
                if (!"\n".equals(seg.getText())) {
                    result.add(seg);
                }
            }
            result.add(new Segment(" ", ruleStyle));
            result.add(new Segment(chars, ruleStyle));
        }

        result.add(Segment.line());
        return result;
    }

    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 0x80) {
                return false;
            }
        }
        return true;
    }

    private static String repeatToWidth(String chars, int width) {
        if (chars.isEmpty() || width <= 0) {
            return "";
        }
        int charCells = Cells.cellLen(chars);
        if (charCells == 0) {
            return "";
        }
        int repeats = width / charCells + 1;
        StringBuilder sb = new StringBuilder(repeats * chars.length());
        for (int i = 0; i < repeats; i++) {
            sb.append(chars);
        }
        return sb.toString();
    }

    private static String setCellSize(String text, int totalCells) {
        return Cells.setCellSize(text, totalCells);
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        return new Measurement(1, 1);
    }

    @Override
    public String toString() {
        return "Rule(" + title + ")";
    }
}
