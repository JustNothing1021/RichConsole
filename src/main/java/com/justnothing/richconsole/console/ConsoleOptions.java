package com.justnothing.richconsole.console;

/**
 * Options for rendering content with a Console.
 * Ported from rich/console.py ConsoleOptions dataclass.
 */
public class ConsoleOptions {

    private final ConsoleDimensions size;
    private final boolean legacyWindows;
    private int minWidth;
    private int maxWidth;
    private final boolean terminal;
    private final String encoding;
    private int maxHeight;
    private String justify;
    private String overflow;
    private Boolean noWrap;
    private Boolean highlight;
    private Boolean markup;
    private Integer height;

    public ConsoleOptions(ConsoleDimensions size, boolean legacyWindows, int minWidth, int maxWidth,
                          boolean terminal, String encoding, int maxHeight,
                          String justify, String overflow, Boolean noWrap,
                          Boolean highlight, Boolean markup, Integer height) {
        this.size = size;
        this.legacyWindows = legacyWindows;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.terminal = terminal;
        this.encoding = encoding;
        this.maxHeight = maxHeight;
        this.justify = justify;
        this.overflow = overflow;
        this.noWrap = noWrap;
        this.highlight = highlight;
        this.markup = markup;
        this.height = height;
    }

    public ConsoleDimensions getSize() {
        return size;
    }

    public boolean isLegacyWindows() {
        return legacyWindows;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public String getEncoding() {
        return encoding;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public String getJustify() {
        return justify;
    }

    public String getOverflow() {
        return overflow;
    }

    public Boolean getNoWrap() {
        return noWrap;
    }

    /**
     * Returns true if no-wrap is enabled.
     * Unlike getNoWrap() which may return null, this returns false when noWrap is null.
     */
    public boolean isNoWrap() {
        return noWrap != null && noWrap;
    }

    public Boolean getHighlight() {
        return highlight;
    }

    public Boolean getMarkup() {
        return markup;
    }

    public Integer getHeight() {
        return height;
    }

    /**
     * Returns true if the encoding is not UTF-based (i.e. ASCII only).
     */
    public boolean isAsciiOnly() {
        return !encoding.startsWith("utf");
    }

    /**
     * Creates a shallow copy of this ConsoleOptions.
     */
    public ConsoleOptions copy() {
        return new ConsoleOptions(size, legacyWindows, minWidth, maxWidth,
                terminal, encoding, maxHeight, justify, overflow, noWrap,
                highlight, markup, height);
    }

    /**
     * Keyword-style builder that returns a copy with non-null values applied.
     * Null parameters mean "no change".
     */
    public ConsoleOptions update(Integer width, Integer minWidth, Integer maxWidth,
                                 String justify, String overflow, Boolean noWrap,
                                 Boolean highlight, Boolean markup, Integer height) {
        ConsoleOptions options = this.copy();
        if (width != null) {
            int w = Math.max(0, width);
            options.minWidth = w;
            options.maxWidth = w;
        }
        if (minWidth != null) {
            options.minWidth = minWidth;
        }
        if (maxWidth != null) {
            options.maxWidth = maxWidth;
        }
        if (justify != null) {
            options.justify = justify;
        }
        if (overflow != null) {
            options.overflow = overflow;
        }
        if (noWrap != null) {
            options.noWrap = noWrap;
        }
        if (highlight != null) {
            options.highlight = highlight;
        }
        if (markup != null) {
            options.markup = markup;
        }
        if (height != null) {
            options.maxHeight = height;
            options.height = Math.max(0, height);
        } else {
            options.maxHeight = this.maxHeight;
            options.height = null;
        }
        return options;
    }

    /**
     * Sets both minWidth and maxWidth to the given width.
     */
    public ConsoleOptions updateWidth(int width) {
        ConsoleOptions options = this.copy();
        int w = Math.max(0, width);
        options.minWidth = w;
        options.maxWidth = w;
        return options;
    }

    /**
     * Sets both maxHeight and height to the given height.
     */
    public ConsoleOptions updateHeight(int height) {
        ConsoleOptions options = this.copy();
        options.maxHeight = height;
        options.height = height;
        return options;
    }

    /**
     * Resets the height override to null.
     */
    public ConsoleOptions resetHeight() {
        ConsoleOptions options = this.copy();
        options.height = null;
        return options;
    }

    /**
     * Sets both width and height dimensions.
     */
    public ConsoleOptions updateDimensions(int width, int height) {
        ConsoleOptions options = this.copy();
        int w = Math.max(0, width);
        options.minWidth = w;
        options.maxWidth = w;
        options.height = height;
        options.maxHeight = height;
        return options;
    }

    /**
     * Clear the height constraint, allowing unlimited vertical space.
     */
    public ConsoleOptions clearHeight() {
        ConsoleOptions options = this.copy();
        options.height = null;
        options.maxHeight = Integer.MAX_VALUE;
        return options;
    }

    @Override
    public String toString() {
        return "ConsoleOptions(size=" + size
                + ", legacyWindows=" + legacyWindows
                + ", minWidth=" + minWidth
                + ", maxWidth=" + maxWidth
                + ", terminal=" + terminal
                + ", encoding=" + encoding
                + ", maxHeight=" + maxHeight
                + ", justify=" + justify
                + ", overflow=" + overflow
                + ", noWrap=" + noWrap
                + ", highlight=" + highlight
                + ", markup=" + markup
                + ", height=" + height + ")";
    }
}
