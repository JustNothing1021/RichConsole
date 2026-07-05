package com.justnothing.richconsole.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.cells.Cells;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.containers.Lines;
import com.justnothing.richconsole.markup.Markup;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Rich text with style spans.
 * Ported from rich/text.py Text class.
 *
 * <p>Text stores plain text content plus a list of {@link Span} objects that
 * associate ranges of the text with styles. It is the primary data structure
 * for styled terminal output in Rich.</p>
 */
public class Text implements RichRenderable {

    // =========================================================================
    // Constants
    // =========================================================================

    /** Default overflow mode, matching Python rich's __rich_console__ default. */
    private static final String DEFAULT_OVERFLOW = "fold";

    /** Ellipsis character (U+2026), matching Python rich's default. */
    private static final String ELLIPSIS = "…";

    /** Overflow mode: fold text at width boundary. */
    private static final String OVERFLOW_FOLD = "fold";

    /** Overflow mode: replace overflowing text with ellipsis. */
    private static final String OVERFLOW_ELLIPSIS = "ellipsis";

    /** Overflow mode: ignore overflow, render beyond width. */
    private static final String OVERFLOW_IGNORE = "ignore";

    /** Overflow mode: crop text at width boundary. */
    private static final String OVERFLOW_CROP = "crop";

    /** Newline string used for line splitting. */
    private static final String NEWLINE = "\n";

    // =========================================================================
    // Fields
    // =========================================================================

    /** The text fragments (lazily joined). */
    private final List<String> _text;

    /** Base style for the entire text; can be a String or Style. */
    private Object style;

    /** Justification: "left", "center", "right", "full", "default". */
    private String justify;

    /** Overflow mode: "crop", "fold", "ellipsis", "ignore". */
    private String overflow;

    /** Whether wrapping is disabled. */
    private Boolean noWrap;

    /** String appended after the text when rendered. */
    private String end;

    /** Number of spaces per tab character. */
    private Integer tabSize;

    /** Style spans over the plain text. */
    private List<Span> _spans;

    /** Cached length of the plain text. */
    private int _length;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Configuration class for creating Text instances with optional parameters.
     */
    public static class Config {
        private String text = "";
        private Object style;
        private String justify;
        private String overflow = "fold";
        private String end = "\n";
        private Integer tabSize = 8;
        private Boolean noWrap = false;

        public Config text(String text) { this.text = text; return this; }
        public Config style(Object style) { this.style = style; return this; }
        public Config justify(String justify) { this.justify = justify; return this; }
        public Config overflow(String overflow) { this.overflow = overflow; return this; }
        public Config end(String end) { this.end = end; return this; }
        public Config tabSize(Integer tabSize) { this.tabSize = tabSize; return this; }
        public Config noWrap(Boolean noWrap) { this.noWrap = noWrap; return this; }
    }

    // =========================================================================
    // Static factory methods (Config-based)
    // =========================================================================

    /**
     * Create a Text using a Configurer callback with an initial text.
     *
     * @param text       the plain text content
     * @param configurer a consumer that configures the Text settings
     * @return a new Text instance
     */
    public static Text of(String text, Consumer<Config> configurer) {
        Config config = new Config();
        config.text = text;
        configurer.accept(config);
        return new Text(config);
    }

    /**
     * Create a Text using a Configurer callback (with text defaulting to "").
     *
     * @param configurer a consumer that configures the Text settings
     * @return a new Text instance
     */
    public static Text of(Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Text(config);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private Text(Config config) {
        this(config.text, config.style, config.justify, config.overflow, config.end, config.tabSize, config.noWrap);
    }

    /**
     * Create an empty Text.
     */
    public Text() {
        this("", null, null, null, null, null, null);
    }

    /**
     * Create a Text with the given plain text.
     */
    public Text(String text) {
        this(text, null, null, null, null, null, null);
    }

    /**
     * Create a Text with the given plain text and style.
     */
    public Text(String text, Object style) {
        this(text, style, null, null, null, null, null);
    }

    /**
     * Full constructor.
     *
     * @param text     the plain text content
     * @param style    base style (String or Style), may be null
     * @param justify  justification mode, may be null
     * @param overflow overflow mode, may be null
     * @param end      string appended after the text (default "\\n")
     * @param tabSize  number of spaces per tab, may be null
     * @param noWrap   whether wrapping is disabled, may be null
     */
    public Text(String text, Object style, String justify, String overflow,
                String end, Integer tabSize, Boolean noWrap) {
        this._text = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            this._text.add(text);
            this._length = text.length();
        } else {
            this._length = 0;
        }
        this.style = style;
        this.justify = justify;
        this.overflow = overflow;
        this.end = end != null ? end : "\n";
        this.tabSize = tabSize;
        this.noWrap = noWrap;
        this._spans = new ArrayList<>();
    }

    // =========================================================================
    // Static factory methods
    // =========================================================================

    /**
     * Create a Text from markup string.
     *
     * @param markup the markup string (e.g., "[bold]Hello[/bold]")
     * @return a Text instance with spans parsed from markup
     */
    public static Text fromMarkup(String markup) {
        return fromMarkup(markup, null);
    }

    /**
     * Create a Text from markup string with a base style.
     *
     * @param markup the markup string
     * @param style  base style (String or Style), may be null
     * @return a Text instance with spans parsed from markup
     */
    public static Text fromMarkup(String markup, Object style) {
        if (markup == null || markup.isEmpty()) {
            return new Text("", style);
        }

        List<Markup.StyledSpan> parsed = Markup.render(markup,
                style instanceof String ? (String) style : null, true);

        StringBuilder plainBuilder = new StringBuilder();
        List<Span> spans = new ArrayList<>();
        int offset = 0;

        for (Markup.StyledSpan styledSpan : parsed) {
            String spanText = styledSpan.text();
            String spanStyle = styledSpan.style();
            int spanStart = offset;
            int spanEnd = offset + spanText.length();
            plainBuilder.append(spanText);

            if (spanStyle != null && !spanStyle.isEmpty()) {
                // Combine base style if it's a Style object
                Object effectiveStyle;
                if (style instanceof Style) {
                    Style parsedSpanStyle = Style.parse(spanStyle);
                    effectiveStyle = ((Style) style).add(parsedSpanStyle);
                } else {
                    effectiveStyle = spanStyle;
                }
                spans.add(new Span(spanStart, spanEnd, effectiveStyle));
            } else if (style != null) {
                spans.add(new Span(spanStart, spanEnd, style));
            }

            offset = spanEnd;
        }

        Text text = new Text(plainBuilder.toString(), style);
        text._spans = spans;
        return text;
    }

    /**
     * Create a Text with a single style applied to the entire text.
     *
     * @param text  the plain text
     * @param style the style (String or Style)
     * @return a styled Text
     */
    public static Text styled(String text, Object style) {
        Text result = new Text(text);
        if (style != null) {
            result.stylize(style, 0, result.length());
        }
        return result;
    }

    /**
     * Assemble a Text from alternating strings and styles.
     * For example: assemble("hello", "bold", " world", "italic")
     * produces Text with "hello" in bold and " world" in italic.
     * If an odd number of arguments is given, the last string has no style.
     *
     * @param parts alternating strings and style objects
     * @return an assembled Text
     */
    public static Text assemble(Object... parts) {
        Text text = new Text();
        int i = 0;
        while (i < parts.length) {
            String content = String.valueOf(parts[i]);
            Object partStyle = null;
            if (i + 1 < parts.length && parts[i + 1] != null) {
                // Check if next part is a style (String style def or Style object)
                if (parts[i + 1] instanceof Style || parts[i + 1] instanceof String) {
                    // Heuristic: if the string after this one looks like a style, use it
                    // But we can't always tell, so we only treat Style instances and
                    // known non-text strings as styles. For simplicity, treat every
                    // second argument as a potential style.
                    partStyle = parts[i + 1];
                    i += 2;
                } else {
                    i++;
                }
            } else {
                i++;
            }
            text.append(content, partStyle);
        }
        return text;
    }

    // =========================================================================
    // Basic properties
    // =========================================================================

    /**
     * Get the character length of the plain text.
     */
    public int length() {
        return _length;
    }

    /**
     * Get the plain text content.
     */
    public String getPlain() {
        if (_text.size() == 1) {
            return _text.get(0);
        }
        return String.join("", _text);
    }

    /**
     * Set the plain text content, replacing all existing text.
     * Spans are cleared.
     */
    public void setPlain(String text) {
        _text.clear();
        if (text != null && !text.isEmpty()) {
            _text.add(text);
            _length = text.length();
        } else {
            _length = 0;
        }
        _spans.clear();
    }

    /**
     * Get the style spans.
     */
    public List<Span> getSpans() {
        return Collections.unmodifiableList(_spans);
    }

    /**
     * Set the style spans.
     */
    public void setSpans(List<Span> spans) {
        this._spans = spans != null ? new ArrayList<>(spans) : new ArrayList<>();
    }

    /**
     * Get the base style.
     */
    public Object getStyle() {
        return style;
    }

    /**
     * Set the base style.
     */
    public void setStyle(Object style) {
        this.style = style;
    }

    /**
     * Get the justification mode.
     */
    public String getJustify() {
        return justify;
    }

    /**
     * Set the justification mode.
     */
    public void setJustify(String justify) {
        this.justify = justify;
    }

    /**
     * Get the overflow mode.
     */
    public String getOverflow() {
        return overflow;
    }

    /**
     * Set the overflow mode.
     */
    public void setOverflow(String overflow) {
        this.overflow = overflow;
    }

    /**
     * Get whether wrapping is disabled.
     */
    public Boolean getNoWrap() {
        return noWrap;
    }

    /**
     * Set whether wrapping is disabled.
     */
    public void setNoWrap(Boolean noWrap) {
        this.noWrap = noWrap;
    }

    /**
     * Get the end string.
     */
    public String getEnd() {
        return end;
    }

    /**
     * Set the end string.
     */
    public void setEnd(String end) {
        this.end = end;
    }

    /**
     * Get the tab size.
     */
    public Integer getTabSize() {
        return tabSize;
    }

    /**
     * Set the tab size.
     */
    public void setTabSize(Integer tabSize) {
        this.tabSize = tabSize;
    }

    /**
     * Get the cell length (display width) of the plain text.
     */
    public int getCellLength() {
        return Cells.cellLen(getPlain());
    }

    // =========================================================================
    // Copy methods
    // =========================================================================

    /**
     * Create a copy of this Text with different plain text but same metadata.
     *
     * @param plain the new plain text
     * @return a new Text with the given plain text and same style/justify/etc.
     */
    public Text blankCopy(String plain) {
        return new Text(plain, style, justify, overflow, end, tabSize, noWrap);
    }

    /**
     * Create a full copy of this Text.
     */
    public Text copy() {
        Text copy = new Text(getPlain(), style, justify, overflow, end, tabSize, noWrap);
        copy._spans = new ArrayList<>(_spans);
        return copy;
    }

    // =========================================================================
    // Stylize methods
    // =========================================================================

    /**
     * Apply a style to a range of the text.
     *
     * @param style the style to apply (String or Style)
     * @param start start offset (inclusive)
     * @param end   end offset (exclusive)
     */
    public void stylize(Object style, int start, int end) {
        if (start >= end || start >= _length) {
            return;
        }
        end = Math.min(end, _length);
        _spans.add(new Span(start, end, style));
    }

    /**
     * Apply a style to the entire text.
     */
    public void stylize(Object style) {
        stylize(style, 0, _length);
    }

    /**
     * Apply a style before existing styles in the given range.
     * Unlike stylize, which adds a span that is combined on top,
     * stylizeBefore inserts a style that forms the base for subsequent spans.
     *
     * @param style the style to apply
     * @param start start offset (inclusive)
     * @param end   end offset (exclusive)
     */
    public void stylizeBefore(Object style, int start, int end) {
        if (start >= end || start >= _length) {
            return;
        }
        end = Math.min(end, _length);
        // Insert at the beginning so it forms a base for later spans
        _spans.add(0, new Span(start, end, style));
    }

    // =========================================================================
    // Append methods
    // =========================================================================

    /**
     * Append text with an optional style.
     *
     * @param text  the text to append (String or Text)
     * @param style the style to apply (String or Style), may be null
     */
    public void append(Object text, Object style) {
        if (text == null) {
            return;
        }
        if (text instanceof Text) {
            appendText((Text) text);
            return;
        }
        String content = text.toString();
        if (content.isEmpty()) {
            return;
        }
        int offset = _length;
        _text.add(content);
        _length += content.length();
        if (style != null) {
            _spans.add(new Span(offset, _length, style));
        }
    }

    /**
     * Append text with no style.
     */
    public void append(Object text) {
        append(text, null);
    }

    /**
     * Append another Text object to this one, merging its spans.
     *
     * @param other the Text to append
     */
    public void appendText(Text other) {
        if (other == null) {
            return;
        }
        int offset = _length;
        String otherPlain = other.getPlain();
        if (!otherPlain.isEmpty()) {
            _text.add(otherPlain);
            _length += otherPlain.length();
        }
        for (Span span : other._spans) {
            _spans.add(span.move(offset));
        }
    }

    /**
     * Append tokens (pairs of [text, style]).
     * Each token is a two-element array: [String text, Object style].
     * If a token has only one element, it is appended as plain text.
     *
     * @param tokens list of token arrays
     */
    public void appendTokens(List<Object[]> tokens) {
        if (tokens == null) {
            return;
        }
        for (Object[] token : tokens) {
            if (token == null || token.length == 0) {
                continue;
            }
            String text = String.valueOf(token[0]);
            Object tokenStyle = token.length > 1 ? token[1] : null;
            append(text, tokenStyle);
        }
    }

    // =========================================================================
    // Padding and alignment
    // =========================================================================

    /**
     * Pad the left side of the text with characters.
     *
     * @param count     number of padding characters
     * @param character the padding character (default " ")
     */
    public void padLeft(int count, String character) {
        if (count <= 0) {
            return;
        }
        String pad = repeatChar(character != null ? character : " ", count);
        int offset = pad.length();
        _text.add(0, pad);
        _length += offset;
        // Move all existing spans forward
        List<Span> newSpans = new ArrayList<>();
        for (Span span : _spans) {
            newSpans.add(span.move(offset));
        }
        _spans = newSpans;
    }

    /**
     * Pad the left side of the text with spaces.
     */
    public void padLeft(int count) {
        padLeft(count, " ");
    }

    /**
     * Pad the right side of the text with characters.
     *
     * @param count     number of padding characters
     * @param character the padding character (default " ")
     */
    public void padRight(int count, String character) {
        if (count <= 0) {
            return;
        }
        String pad = repeatChar(character != null ? character : " ", count);
        _text.add(pad);
        _length += pad.length();
    }

    /**
     * Pad the right side of the text with spaces.
     */
    public void padRight(int count) {
        padRight(count, " ");
    }

    /**
     * Pad both sides of the text equally (centering).
     *
     * @param count     number of padding characters on each side
     * @param character the padding character
     */
    public void pad(int count, String character) {
        padLeft(count, character);
        padRight(count, character);
    }

    /**
     * Pad both sides with spaces.
     */
    public void pad(int count) {
        pad(count, " ");
    }

    /**
     * Align the text to the given width.
     *
     * @param align     alignment direction ("left", "center", "right")
     * @param width     target width in cells
     * @param character the padding character
     */
    public void align(String align, int width, String character) {
        String padChar = character != null ? character : " ";
        int cellLen = getCellLength();
        if (cellLen >= width) {
            return;
        }
        int space = width - cellLen;

        if ("right".equals(align)) {
            padLeft(space, padChar);
        } else if ("center".equals(align)) {
            int left = space / 2;
            int right = space - left;
            padLeft(left, padChar);
            padRight(right, padChar);
        } else if ("full".equals(align)) {
            justifyFull(width);
        } else {
            // "left" or default
            padRight(space, padChar);
        }
    }

    /**
     * Justify text to the given width by distributing extra spaces between words.
     * Only works for single-line text with space-separated words.
     * Simplified: rebuilds text without preserving span offsets.
     */
    private void justifyFull(int width) {
        int cellLen = getCellLength();
        if (cellLen >= width) return;

        String plain = getPlain().trim();
        String[] words = plain.split("\\s+");
        if (words.length <= 1) {
            padRight(width - cellLen, " ");
            return;
        }

        // Calculate total word characters
        int totalWordLen = 0;
        for (String w : words) totalWordLen += Cells.cellLen(w);

        int totalSpaces = width - totalWordLen;
        if (totalSpaces <= 0) return;

        int gaps = words.length - 1;
        int spacesPerGap = totalSpaces / gaps;
        int extraSpaces = totalSpaces % gaps;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append(words[i]);
            if (i < gaps) {
                int spaces = spacesPerGap + (i < extraSpaces ? 1 : 0);
                sb.append(" ".repeat(spaces));
            }
        }

        // Replace text content (simplified — doesn't preserve span offsets)
        _text.clear();
        _text.add(sb.toString());
        // Re-apply the base style to the entire text
        if (style != null) {
            _spans.clear();
            _spans.add(new Span(0, sb.length(), style));
        }
    }

    // =========================================================================
    // Truncation
    // =========================================================================

    /**
     * Truncate the text to fit within the given width.
     *
     * @param maxWidth maximum width in cells
     * @param overflow overflow mode ("crop", "fold", "ellipsis", "ignore"), may be null
     * @param pad      whether to pad if text is shorter than maxWidth
     */
    public void truncate(int maxWidth, String overflow, boolean pad) {
        String overflowMode = overflow != null ? overflow : this.overflow;
        if (overflowMode == null) {
            overflowMode = DEFAULT_OVERFLOW;
        }

        int cellLen = getCellLength();
        if (cellLen <= maxWidth) {
            if (pad) {
                int diff = maxWidth - cellLen;
                if (diff > 0) {
                    padRight(diff);
                }
            }
            return;
        }

        switch (overflowMode) {
            case OVERFLOW_ELLIPSIS -> {
                String plain = getPlain();
                String ellipsis = ELLIPSIS;
                int ellipsisLen = Cells.cellLen(ellipsis);
                int targetLen = maxWidth - ellipsisLen;
                if (targetLen <= 0) {
                    setPlain(ellipsis.substring(0, Math.min(maxWidth, ellipsis.length())));
                    return;
                }
                String[] split = Cells.splitText(plain, targetLen);
                setPlain(split[0] + ellipsis);
            }
            case OVERFLOW_FOLD -> {
                // Fold inserts newlines - for truncate we just crop
                String plain = getPlain();
                String[] split = Cells.splitText(plain, maxWidth);
                setPlain(split[0]);
            }
            case OVERFLOW_IGNORE -> {
                // Do nothing
                return;
            }
            default -> {
                // OVERFLOW_CROP (default)
                String plain = getPlain();
                String[] split = Cells.splitText(plain, maxWidth);
                setPlain(split[0]);
            }
        }

        if (pad) {
            int newCellLen = getCellLength();
            int diff = maxWidth - newCellLen;
            if (diff > 0) {
                padRight(diff);
            }
        }
    }

    // =========================================================================
    // Text manipulation
    // =========================================================================

    /**
     * Strip trailing whitespace from the text.
     */
    public void rstrip() {
        String plain = getPlain();
        int newLen = plain.length();
        while (newLen > 0 && plain.charAt(newLen - 1) <= ' ') {
            newLen--;
        }
        if (newLen < plain.length()) {
            setPlain(plain.substring(0, newLen));
        }
    }

    /**
     * Remove whitespace beyond a certain width at the end of the text.
     * Ported from Python rich's Text.rstrip_end().
     * Only crops excess trailing whitespace that exceeds the given size,
     * does NOT strip all trailing whitespace.
     */
    public void rstripEnd(int size) {
        int textLength = length();
        if (textLength > size) {
            int excess = textLength - size;
            // Count trailing whitespace
            String plain = getPlain();
            int wsCount = 0;
            int i = plain.length() - 1;
            while (i >= 0 && Character.isWhitespace(plain.charAt(i))) {
                wsCount++;
                i--;
            }
            if (wsCount > 0) {
                int cropAmount = Math.min(wsCount, excess);
                rightCrop(textLength - cropAmount);
            }
        }
    }

    /**
     * Expand tab characters to spaces.
     *
     * @param tabSize the number of spaces per tab stop
     */
    public void expandTabs(int tabSize) {
        if (tabSize <= 0) tabSize = 8;
        String plain = getPlain();
        if (!plain.contains("\t")) return;
        StringBuilder sb = new StringBuilder();
        int col = 0;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '\t') {
                int spaces = tabSize - (col % tabSize);
                sb.append("                ", 0, spaces);
                col += spaces;
            } else {
                sb.append(c);
                col++;
            }
        }
        setPlain(sb.toString());
    }

    /**
     * Crop the right side of the text by the given amount.
     *
     * @param amount number of characters to crop from the right
     */
    public void rightCrop(int amount) {
        if (amount <= 0) {
            return;
        }
        int newLen = Math.max(0, _length - amount);
        setLength(newLen);
    }

    /**
     * Set the text length, cropping or extending as needed.
     *
     * @param newLength the new length
     */
    public void setLength(int newLength) {
        if (newLength < 0) {
            newLength = 0;
        }
        if (newLength >= _length) {
            // Extend with spaces
            int diff = newLength - _length;
            if (diff > 0) {
                padRight(diff);
            }
            return;
        }
        // Crop
        String plain = getPlain();
        String newPlain = plain.substring(0, newLength);
        _text.clear();
        _text.add(newPlain);
        _length = newLength;

        // Crop spans
        List<Span> newSpans = new ArrayList<>();
        for (Span span : _spans) {
            Span cropped = span.rightCrop(newLength);
            if (cropped.isValid()) {
                newSpans.add(cropped);
            }
        }
        _spans = newSpans;
    }

    /**
     * Extend the style of the last span by the given number of spaces.
     * If there are no spans, does nothing.
     *
     * @param spaces number of spaces to extend the last style
     */
    public void extendStyle(int spaces) {
        if (spaces <= 0 || _spans.isEmpty()) {
            return;
        }
        // Extend the last span
        int lastIdx = _spans.size() - 1;
        Span last = _spans.get(lastIdx);
        _spans.set(lastIdx, last.extend(spaces));
    }

    // =========================================================================
    // Join / Split / Divide
    // =========================================================================

    /**
     * Join a list of Text objects with a separator.
     * This is the static equivalent of Python's separator.join(texts).
     *
     * @param separator the Text to insert between items
     * @param texts     the list of Text objects to join
     * @return a new Text that is the joined result
     */
    public static Text join(Text separator, List<Text> texts) {
        if (texts == null || texts.isEmpty()) {
            return new Text("");
        }
        // Start with a blank copy of the separator (like Python's blank_copy),
        // which preserves separator's metadata (end, justify, etc.)
        Text result = new Text("", separator.style, separator.justify,
                separator.overflow, separator.end, separator.tabSize, separator.noWrap);
        boolean first = true;
        for (Text text : texts) {
            if (!first && !separator.getPlain().isEmpty()) {
                result.appendText(separator);
            }
            result.appendText(text);
            first = false;
        }
        return result;
    }

    /**
     * Join a list of Text objects with this Text as separator.
     * Similar to str.join().
     *
     * @param texts the list of Text objects to join
     * @return a new Text that is the joined result
     */
    public Text join(List<Text> texts) {
        if (texts == null || texts.isEmpty()) {
            return new Text();
        }

        Text result = new Text();
        boolean first = true;
        for (Text part : texts) {
            if (!first) {
                result.appendText(this);
            }
            result.appendText(part);
            first = false;
        }
        return result;
    }

    /**
     * Split the text by a separator string.
     *
     * @param separator         the separator string
     * @param includeSeparator  whether to include the separator at the end of each split
     * @param allowBlank        whether to include blank results
     * @return a Lines container with the split Text objects
     */
    public Lines split(String separator, boolean includeSeparator, boolean allowBlank) {
        String sep = separator != null ? separator : "\n";
        String plain = getPlain();

        Lines lines = new Lines();

        if (sep.isEmpty()) {
            // Split into individual characters
            for (int i = 0; i < plain.length(); i++) {
                Text charText = blankCopy(plain.substring(i, i + 1));
                // Adjust spans for this character
                adjustSpansForSubstring(charText, i, i + 1);
                lines.append(charText);
            }
            return lines;
        }

        int start = 0;
        while (start <= plain.length()) {
            int idx = plain.indexOf(sep, start);
            if (idx < 0) {
                // Rest of string
                String part = plain.substring(start);
                if (allowBlank || !part.isEmpty()) {
                    Text partText = blankCopy(part);
                    adjustSpansForSubstring(partText, start, plain.length());
                    lines.append(partText);
                }
                break;
            }

            String part = plain.substring(start, idx);
            if (allowBlank || !part.isEmpty()) {
                Text partText = blankCopy(part);
                adjustSpansForSubstring(partText, start, idx);
                lines.append(partText);
            }

            start = idx + sep.length();

            if (includeSeparator) {
                // Append the separator to the last text
                int lineCount = lines.size();
                if (lineCount > 0) {
                    Text lastLine = (Text) lines.get(lineCount - 1);
                    lastLine.append(sep);
                }
            }
        }

        return lines;
    }

    /**
     * Split by newlines.
     */
    public Lines split() {
        return split(NEWLINE, false, false);
    }

    /**
     * Divide the text at the given character offsets.
     * Returns a Lines with (len(offsets) + 1) Text objects.
     *
     * @param offsets the character offsets at which to divide
     * @return a Lines container with the divided Text objects
     */
    public Lines divide(List<Integer> offsets) {
        if (offsets == null || offsets.isEmpty()) {
            Lines lines = new Lines();
            lines.append(copy());
            return lines;
        }

        String plain = getPlain();
        Lines lines = new Lines();

        int prev = 0;
        for (int offset : offsets) {
            offset = Math.min(offset, _length);
            if (offset < prev) {
                offset = prev;
            }
            String part = plain.substring(prev, offset);
            Text partText = blankCopy(part);
            adjustSpansForSubstring(partText, prev, offset);
            lines.append(partText);
            prev = offset;
        }
        // Last segment
        String lastPart = plain.substring(prev);
        Text lastText = blankCopy(lastPart);
        adjustSpansForSubstring(lastText, prev, _length);
        lines.append(lastText);

        return lines;
    }

    // =========================================================================
    // Wrap / Fit
    // =========================================================================

    /**
     * Wrap the text to the given width using word-aware wrapping.
     * Ported from Python rich's _wrap.divide_line logic.
     *
     * @param console  the console
     * @param width    the maximum width in cells
     * @param justify  justification mode, may be null
     * @param overflow overflow mode, may be null
     * @param tabSize  tab size
     * @param noWrap   whether wrapping is disabled, may be null
     * @return a Lines container with wrapped Text objects
     */
    public Lines wrap(Console console, int width, String justify, String overflow,
                      int tabSize, Boolean noWrap) {
        // TODO: use console for width measurement
        String effectiveOverflow = overflow != null ? overflow : this.overflow;
        Boolean effectiveNoWrap = noWrap != null ? noWrap : this.noWrap;

        if (width <= 0) {
            Lines lines = new Lines();
            lines.append(this);
            return lines;
        }

        // If noWrap or overflow is "ignore", don't wrap
        if (effectiveNoWrap != null && effectiveNoWrap) {
            Lines lines = split(NEWLINE, false, true);
            if (OVERFLOW_ELLIPSIS.equals(effectiveOverflow)) {
                for (int i = 0; i < lines.size(); i++) {
                    Text line = (Text) lines.get(i);
                    line.truncate(width, effectiveOverflow, false);
                    lines.set(i, line);
                }
            }
            return lines;
        }
        if (OVERFLOW_IGNORE.equals(effectiveOverflow)) {
            Lines lines = new Lines();
            lines.append(this);
            return lines;
        }

        // First, expand tabs
        Text workText = this.copy();
        workText.expandTabs(tabSize);

        // Split on existing newlines first
        Lines rawLines = workText.split(NEWLINE, false, true);

        boolean doFold = OVERFLOW_FOLD.equals(effectiveOverflow);
        Lines wrappedLines = new Lines();
        for (Object lineObj : rawLines) {
            Text line = (Text) lineObj;
            if (line.getCellLength() <= width) {
                wrappedLines.append(line);
                continue;
            }
            // Use divideLine to compute break positions, then divide to split
            // This preserves spans across line breaks (matching Python rich's behavior)
            List<Integer> offsets = divideLine(line.getPlain(), width, doFold);
            Lines divided = line.divide(offsets);
            for (Object divObj : divided) {
                Text divLine = (Text) divObj;
                divLine.rstripEnd(width);
                wrappedLines.append(divLine);
            }
        }

        // Apply justify if specified
        if (justify != null && !justify.isEmpty()) {
            for (int i = 0; i < wrappedLines.size(); i++) {
                Text line = (Text) wrappedLines.get(i);
                line.align(justify, width, null);
            }
        }

        // Apply overflow truncation
        if (!OVERFLOW_FOLD.equals(effectiveOverflow)) {
            for (int i = 0; i < wrappedLines.size(); i++) {
                Text line = (Text) wrappedLines.get(i);
                line.truncate(width, effectiveOverflow, false);
            }
        }

        // Rstrip trailing whitespace on each line
        for (int i = 0; i < wrappedLines.size(); i++) {
            Text line = (Text) wrappedLines.get(i);
            line.rstripEnd(width);
        }

        return wrappedLines;
    }

    /**
     * Fit the text to the given width by wrapping.
     *
     * @param width the maximum width in cells
     * @return a Lines container with wrapped Text objects
     */
    public Lines fit(int width) {
        return wrap(null, width, justify, overflow,
                tabSize != null ? tabSize : 8, noWrap);
    }

    // =========================================================================
    // Markup
    // =========================================================================

    /**
     * Reconstruct a markup string from this Text's content and spans.
     *
     * @return a markup string that would produce this Text when parsed
     */
    public String getMarkup() {
        String plain = getPlain();
        if (_spans.isEmpty()) {
            return escapeMarkup(plain);
        }

        // Sort spans by start, then by length (longer first for same start)
        List<Span> sortedSpans = new ArrayList<>(_spans);
        Collections.sort(sortedSpans, (a, b) -> {
            if (a.start() != b.start()) {
                return Integer.compare(a.start(), b.start());
            }
            return Integer.compare(b.end(), a.end()); // longer first
        });

        StringBuilder sb = new StringBuilder();
        int pos = 0;

        for (Span span : sortedSpans) {
            if (!span.isValid()) {
                continue;
            }
            // Text before this span
            if (pos < span.start()) {
                sb.append(escapeMarkup(plain.substring(pos, span.start())));
            }
            // Opening tag
            sb.append('[');
            sb.append(styleToString(span.style()));
            sb.append(']');
            // Span text
            sb.append(escapeMarkup(plain.substring(span.start(), Math.min(span.end(), plain.length()))));
            // Closing tag
            sb.append("[/");
            sb.append(styleToString(span.style()));
            sb.append(']');

            pos = Math.max(pos, span.end());
        }

        // Remaining text after all spans
        if (pos < plain.length()) {
            sb.append(escapeMarkup(plain.substring(pos)));
        }

        return sb.toString();
    }

    // =========================================================================
    // Rendering (RichRenderable implementation)
    // =========================================================================

    /**
     * Render this Text to segments for the console.
     * Implements {@link RichRenderable#richConsole(Console, ConsoleOptions)}.
     *
     * @param console the console
     * @param options the console options
     * @return an iterable of Segment objects
     */
    @Override
    public Iterable<Segment> richConsole(Console console, ConsoleOptions options) {
        int tabSize = this.tabSize != null ? this.tabSize : console.getTabSize();
        String justify = this.justify != null ? this.justify : options.getJustify();
        String overflow = this.overflow != null ? this.overflow
                : (options.getOverflow() != null ? options.getOverflow() : DEFAULT_OVERFLOW);
        boolean noWrap = (this.noWrap != null && this.noWrap) || options.isNoWrap();

        Lines lines = wrap(console, options.getMaxWidth(), justify, overflow, tabSize, noWrap);
        if (lines.size() == 1) {
            return ((Text) lines.get(0)).render(console, end);
        }
        List<Text> textLines = new ArrayList<>();
        for (Object obj : lines) {
            textLines.add((Text) obj);
        }
        Text allLines = join(new Text(NEWLINE), textLines);
        return allLines.render(console, end);
    }

    /**
     * Measure the minimum and maximum width of this text.
     *
     * @param console the console (used for style resolution in full implementation)
     * @param options the console options (used for maxWidth capping)
     * @return a Measurement with min and max widths
     */
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        String plain = getPlain();
        int minWidth = 0;
        int maxWidth = 0;

        // Calculate based on lines
        int lineStart = 0;
        for (int i = 0; i <= plain.length(); i++) {
            if (i == plain.length() || plain.charAt(i) == '\n') {
                String line = plain.substring(lineStart, i);
                int lineLen = Cells.cellLen(line);
                if (lineLen > maxWidth) {
                    maxWidth = lineLen;
                }
                // For minWidth, find the minimum non-empty line
                if (minWidth == 0 || (lineLen > 0 && lineLen < minWidth)) {
                    minWidth = lineLen;
                }
                lineStart = i + 1;
            }
        }

        // Cap maxWidth to the console's available width
        if (options != null && options.getMaxWidth() > 0) {
            maxWidth = Math.min(maxWidth, options.getMaxWidth());
        }

        return new Measurement(minWidth, maxWidth);
    }

    /**
     * Render this Text to a list of Segments.
     *
     * @param console the console (used for resolving string-based style names)
     * @param end     the string to append after the text (e.g., "\\n")
     * @return an iterable of Segment objects
     */
    public Iterable<Segment> render(Console console, String end) {
        List<Segment> segments = new ArrayList<>();
        String plain = getPlain();

        if (plain.isEmpty()) {
            if (end != null && !end.isEmpty()) {
                segments.add(new Segment(end, resolveStyle(style, console)));
            }
            return segments;
        }

        // Resolve the base style
        Style baseStyle = resolveStyle(style, console);

        // Build a list of (start, end, resolvedStyle) tuples from spans
        List<int[]> spanRanges = new ArrayList<>();
        List<Style> spanStyles = new ArrayList<>();
        for (Span span : _spans) {
            if (span.isValid()) {
                spanRanges.add(new int[]{span.start(), Math.min(span.end(), _length)});
                spanStyles.add(resolveStyle(span.style(), console));
            }
        }

        int lineStart = 0;
        for (int i = 0; i <= plain.length(); i++) {
            boolean isEnd = (i == plain.length());
            boolean isNewline = !isEnd && plain.charAt(i) == '\n';

            if (isEnd || isNewline) {
                // Render the current line segment(s)
                if (i > lineStart) {
                    renderLine(segments, plain, lineStart, i, spanRanges, spanStyles);
                }

                if (isNewline) {
                    segments.add(Segment.line());
                    lineStart = i + 1;
                }
            }
        }

        // Append end string
        if (end != null && !end.isEmpty()) {
            segments.add(new Segment(end, baseStyle));
        }

        return segments;
    }

    // =========================================================================
    // Highlight methods
    // =========================================================================

    /**
     * Highlight text matching a regex pattern.
     *
     * @param pattern     the regex pattern string
     * @param style       the style to apply to matches
     */
    public void highlightRegex(String pattern, Object style) {
        highlightRegex(pattern, style, null);
    }

    /**
     * Highlight text matching a regex pattern.
     * If stylePrefix is provided, named groups in the pattern are used
     * to construct style names as stylePrefix + groupName.
     *
     * @param pattern     the regex pattern
     * @param style       the default style to apply to matches (used when no stylePrefix)
     * @param stylePrefix prefix for named group styles (e.g. "repr." → "repr.boolTrue")
     */
    public void highlightRegex(String pattern, Object style, String stylePrefix) {
        if (pattern == null || pattern.isEmpty()) {
            return;
        }
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(getPlain());
        while (m.find()) {
            if (stylePrefix != null) {
                // Apply style based on named groups
                // Extract named group names from pattern string
                java.util.Set<String> groupNames = extractNamedGroups(pattern);
                for (String groupName : groupNames) {
                    try {
                        String groupValue = m.group(groupName);
                        if (groupValue != null) {
                            int start = m.start(groupName);
                            int end = m.end(groupName);
                            stylize(stylePrefix + groupName, start, end);
                        }
                    } catch (IllegalArgumentException e) {
                        // Group not in this pattern, skip
                    }
                }
            } else {
                stylize(style, m.start(), m.end());
            }
        }
    }

    /**
     * Extract named group names from a regex pattern string.
     * Matches (?P&lt;name&gt;...) or (?&lt;name&gt;...) patterns.
     */
    private static java.util.Set<String> extractNamedGroups(String pattern) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        Pattern namedGroupPattern = Pattern.compile("\\(\\?P?<([a-zA-Z][a-zA-Z0-9_]*)>");
        Matcher nm = namedGroupPattern.matcher(pattern);
        while (nm.find()) {
            names.add(nm.group(1));
        }
        return names;
    }

    /**
     * Highlight words in the text.
     *
     * @param words          the words to highlight
     * @param style          the style to apply to matches
     * @param caseSensitive whether matching is case-sensitive
     * @return the number of words highlighted
     */
    public int highlightWords(List<String> words, Object style, boolean caseSensitive) {
        if (words == null || words.isEmpty()) {
            return 0;
        }
        int count = 0;
        String plain = getPlain();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", flags);
            Matcher m = p.matcher(plain);
            while (m.find()) {
                stylize(style, m.start(), m.end());
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // Object methods
    // =========================================================================

    @Override
    public String toString() {
        return getPlain();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Text other)) return false;
        return getPlain().equals(other.getPlain())
                && _spans.equals(other._spans);
    }

    @Override
    public int hashCode() {
        return 31 * getPlain().hashCode() + _spans.hashCode();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolve an Object (String or Style) to a Style instance.
     */
    private Style resolveStyle(Object styleObj) {
        return resolveStyle(styleObj, null);
    }

    /**
     * Resolve an Object (String or Style) to a Style instance,
     * using the console for theme-based style resolution.
     */
    private Style resolveStyle(Object styleObj, Console console) {
        if (styleObj == null) {
            return null;
        }
        if (styleObj instanceof Style s) {
            return s;
        }
        if (styleObj instanceof String styleStr) {
            if (styleStr.isEmpty()) {
                return null;
            }
            // Use console.getStyle() for theme-aware resolution
            if (console != null) {
                return console.getStyle(styleStr);
            }
            try {
                return Style.parse(styleStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Convert a style object to a string for markup output.
     */
    private String styleToString(Object styleObj) {
        if (styleObj == null) {
            return "";
        }
        if (styleObj instanceof String) {
            return (String) styleObj;
        }
        if (styleObj instanceof Style) {
            return styleObj.toString();
        }
        return String.valueOf(styleObj);
    }

    /**
     * Escape special markup characters in text.
     */
    private String escapeMarkup(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("[", "\\[");
    }

    /**
     * Repeat a character string count times.
     */
    private static String repeatChar(String character, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count * character.length());
        for (int i = 0; i < count; i++) {
            sb.append(character);
        }
        return sb.toString();
    }

    /**
     * Adjust spans from this Text for a substring extracted at [start, end).
     * Adds the relevant (cropped and offset-adjusted) spans to the target Text.
     */
    private void adjustSpansForSubstring(Text target, int start, int end) {
        for (Span span : _spans) {
            int spanStart = Math.max(span.start(), start);
            int spanEnd = Math.min(span.end(), end);
            if (spanStart < spanEnd) {
                target._spans.add(new Span(spanStart - start, spanEnd - start, span.style()));
            }
        }
    }

    /**
     * Compute break positions for word-aware line wrapping.
     * Ported from Python rich's _wrap.divide_line.
     * Returns a list of character offsets where the text should be split.
     *
     * @param text  the plain text to analyze
     * @param width the maximum width in cells
     * @param fold  whether to fold long words across multiple lines
     * @return a list of character offsets for line breaks
     */
    private static List<Integer> divideLine(String text, int width, boolean fold) {
        List<Integer> breakPositions = new ArrayList<>();
        int cellOffset = 0;

        // Match words (including trailing whitespace)
        Matcher matcher = RE_WORD.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String word = matcher.group();
            int wordLength = Cells.cellLen(word.trim());
            int remainingSpace = width - cellOffset;
            boolean wordFits = remainingSpace >= wordLength;

            if (wordFits) {
                cellOffset += Cells.cellLen(word);
            } else {
                if (wordLength > width) {
                    // Word is longer than any line
                    if (fold) {
                        List<String> folded = Cells.chopCells(word, width);
                        for (int i = 0; i < folded.size(); i++) {
                            if (start > 0 || i > 0) {
                                breakPositions.add(start);
                            }
                            if (i < folded.size() - 1) {
                                start += folded.get(i).length();
                            } else {
                                cellOffset = Cells.cellLen(folded.get(i));
                            }
                        }
                    } else {
                        if (start > 0) {
                            breakPositions.add(start);
                        }
                        cellOffset = Cells.cellLen(word);
                    }
                } else if (cellOffset > 0 && start > 0) {
                    breakPositions.add(start);
                    cellOffset = Cells.cellLen(word);
                }
            }
        }
        return breakPositions;
    }

    /** Regex to match words with trailing whitespace, matching Python rich's re_word. */
    private static final Pattern RE_WORD = Pattern.compile("\\s*\\S+\\s*");

    /**
     * Render a single line of text with spans applied.
     */
    private void renderLine(List<Segment> segments, String plain, int lineStart, int lineEnd,
                            List<int[]> spanRanges, List<Style> spanStyles) {
        // Collect all style-change points within [lineStart, lineEnd)
        List<int[]> events = new ArrayList<>();
        for (int i = 0; i < spanRanges.size(); i++) {
            int[] range = spanRanges.get(i);
            int spanStart = Math.max(range[0], lineStart);
            int spanEnd = Math.min(range[1], lineEnd);
            if (spanStart < spanEnd) {
                events.add(new int[]{spanStart, i, 1}); // style start
                events.add(new int[]{spanEnd, i, -1});   // style end
            }
        }

        // Sort events by position, then end events before start events at same position
        Collections.sort(events, (a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[2], b[2]); // end (-1) before start (1)
        });

        int pos = lineStart;
        int eventIdx = 0;

        // Track which spans are active
        List<Boolean> activeSpans = new ArrayList<>();
        for (int i = 0; i < spanStyles.size(); i++) {
            activeSpans.add(false);
        }

        while (pos < lineEnd || eventIdx < events.size()) {
            int nextEventPos = lineEnd;
            if (eventIdx < events.size()) {
                nextEventPos = events.get(eventIdx)[0];
            }

            // Emit segment for text before next event
            if (pos < nextEventPos) {
                String text = plain.substring(pos, nextEventPos);
                Style currentStyle = computeCurrentStyle(activeSpans, spanStyles,
                        resolveStyle(style));
                segments.add(new Segment(text, currentStyle));
                pos = nextEventPos;
            }

            // Process events at this position
            while (eventIdx < events.size() && events.get(eventIdx)[0] == pos) {
                int[] event = events.get(eventIdx);
                int spanIdx = event[1];
                if (event[2] == 1) {
                    activeSpans.set(spanIdx, true);
                } else {
                    activeSpans.set(spanIdx, false);
                }
                eventIdx++;
            }

            if (pos >= lineEnd) {
                break;
            }
        }
    }

    /**
     * Compute the effective style by combining all active span styles.
     */
    private Style computeCurrentStyle(List<Boolean> activeSpans, List<Style> spanStyles,
                                       Style baseStyle) {
        Style result = baseStyle;
        if (result == null) {
            result = Style.nullStyle();
        }
        for (int i = 0; i < activeSpans.size(); i++) {
            if (activeSpans.get(i)) {
                Style spanStyle = spanStyles.get(i);
                if (spanStyle != null) {
                    result = result.add(spanStyle);
                }
            }
        }
        return result.isNull() ? null : result;
    }
}
