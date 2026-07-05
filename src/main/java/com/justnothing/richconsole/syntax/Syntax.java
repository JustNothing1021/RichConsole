package com.justnothing.richconsole.syntax;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.containers.Lines;
import com.justnothing.richconsole.padding.Padding;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.text.Text;

/**
 * A renderable for syntax-highlighted code.
 * Ported from rich/syntax.py Syntax class.
 */
public class Syntax implements RichRenderable {

    // =========================================================================
    // Instance fields
    // =========================================================================

    private final String code;
    private final Lexer lexer;
    private final SyntaxTheme theme;
    private final boolean lineNumbers;
    private final int startLine;
    private final Set<Integer> highlightLines;
    private final Integer codeWidth;
    private final int tabSize;
    private final boolean wordWrap;
    private final String backgroundColor;
    private final Object padding;

    // =========================================================================
    // Config — fluent configuration for Syntax construction
    // =========================================================================

    /**
     * Fluent configuration object for Syntax construction.
     * Usage: {@code Syntax.of("code", cfg -> cfg.lexerName("java").lineNumbers(true))}
     */
    public static class Config {
        public String code = "";
        public Lexer lexer;
        public SyntaxTheme theme;
        public boolean lineNumbers = false;
        public int startLine = 1;
        public Set<Integer> highlightLines;
        public Integer codeWidth;
        public int tabSize = 4;
        public boolean wordWrap = false;
        public String backgroundColor;
        public Object padding;
        public String lexerName = "text";
        public String themeName = "monokai";

        public Config code(String code) { this.code = code; return this; }
        public Config lexer(Lexer lexer) { this.lexer = lexer; return this; }
        public Config theme(SyntaxTheme theme) { this.theme = theme; return this; }
        public Config lineNumbers(boolean lineNumbers) { this.lineNumbers = lineNumbers; return this; }
        public Config startLine(int startLine) { this.startLine = startLine; return this; }
        public Config highlightLines(Set<Integer> highlightLines) { this.highlightLines = highlightLines; return this; }
        public Config codeWidth(Integer codeWidth) { this.codeWidth = codeWidth; return this; }
        public Config tabSize(int tabSize) { this.tabSize = tabSize; return this; }
        public Config wordWrap(boolean wordWrap) { this.wordWrap = wordWrap; return this; }
        public Config backgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; return this; }
        public Config padding(Object padding) { this.padding = padding; return this; }
        public Config lexerName(String lexerName) { this.lexerName = lexerName; return this; }
        public Config themeName(String themeName) { this.themeName = themeName; return this; }
    }

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Create a Syntax with fluent configuration.
     * <pre>{@code
     * Syntax.of("class Foo {}", cfg -> cfg.lexerName("java").lineNumbers(true))
     * }</pre>
     */
    public static Syntax of(String code, Consumer<Config> configurer) {
        Config cfg = new Config();
        cfg.code = code != null ? code : "";
        configurer.accept(cfg);
        return new Syntax(cfg);
    }

    // =========================================================================
    // Constructor from Config (private)
    // =========================================================================

    private Syntax(Config cfg) {
        this.code = cfg.code != null ? cfg.code : "";
        this.lexer = cfg.lexer != null ? cfg.lexer : LexerRegistry.get(cfg.lexerName);
        this.theme = cfg.theme != null ? cfg.theme : resolveTheme(cfg.themeName);
        this.lineNumbers = cfg.lineNumbers;
        this.startLine = cfg.startLine;
        this.highlightLines = cfg.highlightLines != null ? cfg.highlightLines : new HashSet<>();
        this.codeWidth = cfg.codeWidth;
        this.tabSize = cfg.tabSize;
        this.wordWrap = cfg.wordWrap;
        this.backgroundColor = cfg.backgroundColor;
        this.padding = cfg.padding;
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    public Syntax(String code, Lexer lexer, SyntaxTheme theme,
                  boolean lineNumbers, int startLine,
                  Set<Integer> highlightLines, Integer codeWidth,
                  int tabSize, boolean wordWrap,
                  String backgroundColor, Object padding) {
        this.code = code != null ? code : "";
        this.lexer = lexer;
        this.theme = theme != null ? theme : SyntaxTheme.MONOKAI;
        this.lineNumbers = lineNumbers;
        this.startLine = startLine;
        this.highlightLines = highlightLines != null ? highlightLines : new HashSet<>();
        this.codeWidth = codeWidth;
        this.tabSize = tabSize;
        this.wordWrap = wordWrap;
        this.backgroundColor = backgroundColor;
        this.padding = padding;
    }

    /**
     * Create Syntax with a lexer name (looked up from registry).
     */
    public Syntax(String code, String lexerName, String themeName,
                  boolean lineNumbers, int startLine,
                  Set<Integer> highlightLines, Integer codeWidth,
                  int tabSize, boolean wordWrap,
                  String backgroundColor, Object padding) {
        this(code,
             LexerRegistry.get(lexerName),
             resolveTheme(themeName),
             lineNumbers, startLine, highlightLines, codeWidth,
             tabSize, wordWrap, backgroundColor, padding);
    }

    /**
     * Simplified constructor.
     */
    public Syntax(String code, String lexerName) {
        this(code, lexerName, "monokai", false, 1, null, null, 4, false, null, 0);
    }

    /**
     * Constructor with highlight lines.
     */
    public Syntax(String code, String lexerName, boolean lineNumbers,
                  int startLine, Set<Integer> highlightLines) {
        this(code, lexerName, "monokai", lineNumbers, startLine, highlightLines,
             null, 4, false, null, 0);
    }

    // =========================================================================
    // Theme resolution
    // =========================================================================

    private static SyntaxTheme resolveTheme(String themeName) {
        if (themeName == null) return SyntaxTheme.MONOKAI;
        return switch (themeName.toLowerCase()) {
            case "dark" -> SyntaxTheme.ANSI_DARK;
            case "light" -> SyntaxTheme.ANSI_LIGHT;
            default -> SyntaxTheme.MONOKAI;
        };
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Iterable<Segment> richConsole(Console console, ConsoleOptions options) {
        // Step 1: Tokenize
        List<SyntaxToken> tokens;
        if (lexer != null) {
            tokens = lexer.tokenize(processCode(this.code));
        } else {
            // No lexer, treat as plain text
            tokens = new ArrayList<>();
            tokens.add(new SyntaxToken(SyntaxTokenType.TEXT, this.code));
        }

        // Step 2: Get background style for the code block
        Style bgStyle = theme.getBackgroundStyle();
        if ((bgStyle == null || bgStyle.isNull()) && backgroundColor != null) {
            bgStyle = Style.parse("on " + backgroundColor);
        }

        // Step 3: Convert tokens to Text with styles
        Text text = new Text();
        text.setStyle(bgStyle); // base style includes background color
        for (SyntaxToken token : tokens) {
            Style tokenStyle = theme.getStyleForToken(token.type);
            text.append(token.text, tokenStyle);
        }
        text.setJustify("left");
        text.setEnd("");
        text.setNoWrap(!this.wordWrap);
        text.setOverflow(this.wordWrap ? "fold" : "crop");

        // Step 4: If no line numbers, render text with background fill
        if (!lineNumbers) {
            int width = codeWidth != null ? codeWidth : options.getMaxWidth();
            List<List<Segment>> lineList = new ArrayList<>();
            for (Object lineObj : text.split()) {
                if (lineObj instanceof Text lineText) {
                    lineText.setEnd("");
                    lineText.setNoWrap(!this.wordWrap);
                    lineText.setOverflow(this.wordWrap ? "fold" : "crop");
                    List<Segment> lineSegs = toSegmentList(lineText.richConsole(console, options));
                    // Pad each line to the full width with background style
                    lineList.add(Segment.adjustLineLength(lineSegs, width, bgStyle, true));
                }
            }
            // Flatten lines with newlines
            List<Segment> result = new ArrayList<>();
            for (int i = 0; i < lineList.size(); i++) {
                result.addAll(lineList.get(i));
                if (i < lineList.size() - 1) {
                    result.add(Segment.line());
                }
            }
            if (padding instanceof Integer pad && pad > 0) {
                Padding padded = new Padding(new Group(result), pad, pad, pad, pad, bgStyle, true);
                return toSegmentList(padded.richConsole(console, options));
            }
            return result;
        }

        // Step 5: Line numbers rendering
        return renderWithLineNumbers(console, options, text, bgStyle);
    }

    // =========================================================================
    // Line numbers
    // =========================================================================

    private Iterable<Segment> renderWithLineNumbers(Console console, ConsoleOptions options,
                                                    Text text, Style bgStyle) {
        // Split text into lines
        Lines linesObj = text.split();
        int totalLines = linesObj.size();
        int lastLineNumber = startLine + totalLines - 1;
        int numbersWidth = String.valueOf(lastLineNumber).length();

        // Get styles — merge with bgStyle for background color
        Style lineNumberStyle = theme.getDefaultStyle().add(bgStyle);
        Style highlightLineStyle = console.getStyle("syntax.highlight_line");
        if (highlightLineStyle == null) {
            highlightLineStyle = lineNumberStyle.add(Style.parse("bold"));
        } else {
            highlightLineStyle = highlightLineStyle.add(bgStyle);
        }

        // Separator style
        Style lineStyle = bgStyle;

        // Separator between line numbers and code
        String separator = " │ "; // │

        // Calculate gutter width (line numbers + separator)
        int gutterWidth = 2 + numbersWidth + separator.length(); // "  " + number + " │ "

        // Calculate code area width
        int targetCodeWidth = this.codeWidth != null ? this.codeWidth
                : Math.max(1, options.getMaxWidth() - gutterWidth);

        // Build the result
        List<Segment> result = new ArrayList<>();

        for (int i = 0; i < linesObj.size(); i++) {
            int lineNumber = startLine + i;
            boolean isHighlighted = highlightLines.contains(lineNumber);

            // Line number prefix
            String numStr = String.format("%" + numbersWidth + "d", lineNumber);
            if (isHighlighted) {
                result.add(new Segment("\u276F ", highlightLineStyle)); // ❱
                result.add(new Segment(numStr, highlightLineStyle));
            } else {
                result.add(new Segment("  ", lineStyle));
                result.add(new Segment(numStr, lineNumberStyle));
            }

            // Separator
            result.add(new Segment(separator, lineStyle));

            // Code content - set end to empty to avoid double newlines
            Object lineObj = linesObj.get(i);
            if (lineObj instanceof Text lineText) {
                lineText.setEnd("");
                lineText.setNoWrap(true);
                lineText.setOverflow("crop");
                List<Segment> codeSegments = new ArrayList<>();
                for (Object seg : lineText.richConsole(console, options)) {
                    if (seg instanceof Segment s) {
                        codeSegments.add(s);
                    }
                }
                // Pad/crop code line to targetCodeWidth with background style
                List<Segment> adjusted = Segment.adjustLineLength(codeSegments, targetCodeWidth, bgStyle, true);
                result.addAll(adjusted);
            } else {
                // Plain text line — pad with background
                result.add(new Segment(spaces(targetCodeWidth), bgStyle));
            }

            // Newline between lines (not after the last line)
            if (i < linesObj.size() - 1) {
                result.add(Segment.line());
            }
        }

        // Apply padding with background style
        if (padding instanceof Integer pad && pad > 0) {
            Padding padded = new Padding(new Group(result), pad, pad, pad, pad, bgStyle, true);
            return toSegmentList(padded.richConsole(console, options));
        }

        return result;
    }

    // Simple Group helper to wrap a Segment list as RichRenderable
    private static class Group implements RichRenderable {
        private final List<Segment> segments;

        Group(List<Segment> segments) {
            this.segments = segments;
        }

        @Override
        public Iterable<?> richConsole(Console console, ConsoleOptions options) {
            return segments;
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static String spaces(int count) {
        return " ".repeat(Math.max(0, count));
    }

    private String processCode(String code) {
        String result = code;
        // Expand tabs
        if (tabSize > 0 && result.contains("\t")) {
            result = result.replace("\t", " ".repeat(tabSize));
        }
        // Ensure code ends with newline for consistent line counting
        if (!result.isEmpty() && !result.endsWith("\n")) {
            result = result + "\n";
        }
        return result;
    }

    private static List<Segment> toSegmentList(Iterable<?> iterable) {
        List<Segment> list = new ArrayList<>();
        if (iterable != null) {
            for (Object item : iterable) {
                if (item instanceof Segment s) {
                    list.add(s);
                }
            }
        }
        return list;
    }
}
