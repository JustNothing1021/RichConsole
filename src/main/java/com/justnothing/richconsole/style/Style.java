package com.justnothing.richconsole.style;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.justnothing.richconsole.color.Color;
import com.justnothing.richconsole.color.ColorSystem;
import com.justnothing.richconsole.color.ColorTriplet;
import com.justnothing.richconsole.errors.StyleSyntaxError;
import com.justnothing.richconsole.terminal.TerminalTheme;

/**
 * Terminal style definition with bit-field attribute storage.
 * Ported from rich/style.py.
 */
public class Style {

    // Bit positions for style attributes
    static final int BIT_BOLD = 0;
    static final int BIT_DIM = 1;
    static final int BIT_ITALIC = 2;
    static final int BIT_UNDERLINE = 3;
    static final int BIT_BLINK = 4;
    static final int BIT_BLINK2 = 5;
    static final int BIT_REVERSE = 6;
    static final int BIT_CONCEAL = 7;
    static final int BIT_STRIKE = 8;
    static final int BIT_UNDERLINE2 = 9;
    static final int BIT_FRAME = 10;
    static final int BIT_ENCIRCLE = 11;
    static final int BIT_OVERLINE = 12;

    /**
     * Map from bit position to SGR ANSI code string.
     */
    static final Map<Integer, String> STYLE_MAP;
    static {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(BIT_BOLD, "1");
        m.put(BIT_DIM, "2");
        m.put(BIT_ITALIC, "3");
        m.put(BIT_UNDERLINE, "4");
        m.put(BIT_BLINK, "5");
        m.put(BIT_BLINK2, "6");
        m.put(BIT_REVERSE, "7");
        m.put(BIT_CONCEAL, "8");
        m.put(BIT_STRIKE, "9");
        m.put(BIT_UNDERLINE2, "21");
        m.put(BIT_FRAME, "51");
        m.put(BIT_ENCIRCLE, "52");
        m.put(BIT_OVERLINE, "53");
        STYLE_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * Map from style attribute name (and abbreviations) to bit position.
     */
    static final Map<String, Integer> STYLE_ATTRIBUTES;
    static {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("dim", BIT_DIM);
        m.put("d", BIT_DIM);
        m.put("bold", BIT_BOLD);
        m.put("b", BIT_BOLD);
        m.put("italic", BIT_ITALIC);
        m.put("i", BIT_ITALIC);
        m.put("underline", BIT_UNDERLINE);
        m.put("u", BIT_UNDERLINE);
        m.put("blink", BIT_BLINK);
        m.put("blink2", BIT_BLINK2);
        m.put("reverse", BIT_REVERSE);
        m.put("r", BIT_REVERSE);
        m.put("conceal", BIT_CONCEAL);
        m.put("c", BIT_CONCEAL);
        m.put("strike", BIT_STRIKE);
        m.put("s", BIT_STRIKE);
        m.put("underline2", BIT_UNDERLINE2);
        m.put("uu", BIT_UNDERLINE2);
        m.put("frame", BIT_FRAME);
        m.put("encircle", BIT_ENCIRCLE);
        m.put("overline", BIT_OVERLINE);
        m.put("o", BIT_OVERLINE);
        STYLE_ATTRIBUTES = Collections.unmodifiableMap(m);
    }

    /** ANSI reset code. */
    private static final String ANSI_RESET = "\u001B[0m";
    /** ANSI escape prefix. */
    private static final String ESC = "\u001B[";

    // Parse cache with max size
    private static final int PARSE_CACHE_MAX = 4096;
    private static final ConcurrentHashMap<String, Style> PARSE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> NORMALIZE_CACHE = new ConcurrentHashMap<>();

    // Null style singleton
    private static final Style NULL_STYLE = new Style(true);

    // Instance fields
    private final Color color;
    private final Color bgcolor;
    private final int attributes;       // bit field: on/off for each attribute
    private final int setAttributes;    // bit field: which attributes are explicitly set
    private final String link;
    private final String linkId;
    private final Map<String, Object> meta;
    private final boolean nullStyle;

    // Lazy-computed caches
    private volatile String ansi;
    private volatile String styleDefinition;
    private volatile Integer hash;
    private volatile String htmlStyle;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Private constructor for NULL_STYLE singleton.
     */
    private Style(boolean nullStyle) {
        this.nullStyle = nullStyle;
        this.color = null;
        this.bgcolor = null;
        this.attributes = 0;
        this.setAttributes = 0;
        this.link = null;
        this.linkId = null;
        this.meta = null;
    }

    /**
     * Full constructor with all fields.
     */
    public Style(Color color, Color bgcolor,
                 Boolean bold, Boolean dim, Boolean italic, Boolean underline,
                 Boolean blink, Boolean blink2, Boolean reverse, Boolean conceal,
                 Boolean strike, Boolean underline2, Boolean frame, Boolean encircle,
                 Boolean overline,
                 String link, Map<String, Object> meta) {
        this.nullStyle = false;
        this.color = color;
        this.bgcolor = bgcolor;

        int attrs = 0;
        int setAttrs = 0;
        if (bold != null) {
            setAttrs |= (1 << BIT_BOLD);
            if (bold) attrs |= (1 << BIT_BOLD);
        }
        if (dim != null) {
            setAttrs |= (1 << BIT_DIM);
            if (dim) attrs |= (1 << BIT_DIM);
        }
        if (italic != null) {
            setAttrs |= (1 << BIT_ITALIC);
            if (italic) attrs |= (1 << BIT_ITALIC);
        }
        if (underline != null) {
            setAttrs |= (1 << BIT_UNDERLINE);
            if (underline) attrs |= (1 << BIT_UNDERLINE);
        }
        if (blink != null) {
            setAttrs |= (1 << BIT_BLINK);
            if (blink) attrs |= (1 << BIT_BLINK);
        }
        if (blink2 != null) {
            setAttrs |= (1 << BIT_BLINK2);
            if (blink2) attrs |= (1 << BIT_BLINK2);
        }
        if (reverse != null) {
            setAttrs |= (1 << BIT_REVERSE);
            if (reverse) attrs |= (1 << BIT_REVERSE);
        }
        if (conceal != null) {
            setAttrs |= (1 << BIT_CONCEAL);
            if (conceal) attrs |= (1 << BIT_CONCEAL);
        }
        if (strike != null) {
            setAttrs |= (1 << BIT_STRIKE);
            if (strike) attrs |= (1 << BIT_STRIKE);
        }
        if (underline2 != null) {
            setAttrs |= (1 << BIT_UNDERLINE2);
            if (underline2) attrs |= (1 << BIT_UNDERLINE2);
        }
        if (frame != null) {
            setAttrs |= (1 << BIT_FRAME);
            if (frame) attrs |= (1 << BIT_FRAME);
        }
        if (encircle != null) {
            setAttrs |= (1 << BIT_ENCIRCLE);
            if (encircle) attrs |= (1 << BIT_ENCIRCLE);
        }
        if (overline != null) {
            setAttrs |= (1 << BIT_OVERLINE);
            if (overline) attrs |= (1 << BIT_OVERLINE);
        }

        this.attributes = attrs;
        this.setAttributes = setAttrs;
        this.link = link;
        this.linkId = link != null ? Integer.toHexString(System.identityHashCode(link)) : null;
        this.meta = meta != null ? Collections.unmodifiableMap(new LinkedHashMap<>(meta)) : null;
    }

    /**
     * Constructor from StyleBuilder.
     */
    Style(StyleBuilder b) {
        this(b.getColor(), b.getBgcolor(),
             b.getBold(), b.getDim(), b.getItalic(), b.getUnderline(),
             b.getBlink(), b.getBlink2(), b.getReverse(), b.getConceal(),
             b.getStrike(), b.getUnderline2(), b.getFrame(), b.getEncircle(),
             b.getOverline(),
             b.getLink(), b.getMeta());
    }

    /**
     * Internal constructor for creating derived styles (add, copy, etc.).
     */
    private Style(Color color, Color bgcolor, int attributes, int setAttributes,
                  String link, String linkId, Map<String, Object> meta, boolean nullStyle) {
        this.color = color;
        this.bgcolor = bgcolor;
        this.attributes = attributes;
        this.setAttributes = setAttributes;
        this.link = link;
        this.linkId = linkId;
        this.meta = meta != null ? Collections.unmodifiableMap(new LinkedHashMap<>(meta)) : null;
        this.nullStyle = nullStyle;
    }

    // =========================================================================
    // Static factory methods
    // =========================================================================

    /**
     * Returns the null (empty) style singleton.
     */
    public static Style nullStyle() {
        return NULL_STYLE;
    }

    /**
     * Create a new Style builder.
     */
    public static StyleBuilder builder() {
        return new StyleBuilder();
    }

    /**
     * Create a Style from foreground and background colors.
     */
    public static Style fromColor(Color color, Color bgcolor) {
        return new Style(color, bgcolor,
                         null, null, null, null,
                         null, null, null, null,
                         null, null, null, null,
                         null, null, null);
    }

    /**
     * Create a Style from meta data only.
     */
    public static Style fromMeta(Map<String, Object> meta) {
        return new Style(null, null,
                         null, null, null, null,
                         null, null, null, null,
                         null, null, null, null,
                         null, null, meta);
    }

    /**
     * Create a Style by applying meta handlers.
     * Equivalent to Python's Style.on() class method.
     */
    public static Style on(Map<String, Object> meta, Map<String, Object> handlers) {
        Map<String, Object> combinedMeta = meta != null ? new LinkedHashMap<>(meta) : new LinkedHashMap<>();
        if (handlers != null) {
            for (Map.Entry<String, Object> entry : handlers.entrySet()) {
                combinedMeta.put("@" + entry.getKey(), entry.getValue());
            }
        }
        return fromMeta(combinedMeta);
    }

    // =========================================================================
    // Attribute getters (return null if not set, true/false if set)
    // =========================================================================

    public Boolean bold() {
        return getAttribute(BIT_BOLD);
    }

    public Boolean dim() {
        return getAttribute(BIT_DIM);
    }

    public Boolean italic() {
        return getAttribute(BIT_ITALIC);
    }

    public Boolean underline() {
        return getAttribute(BIT_UNDERLINE);
    }

    public Boolean blink() {
        return getAttribute(BIT_BLINK);
    }

    public Boolean blink2() {
        return getAttribute(BIT_BLINK2);
    }

    public Boolean reverse() {
        return getAttribute(BIT_REVERSE);
    }

    public Boolean conceal() {
        return getAttribute(BIT_CONCEAL);
    }

    public Boolean strike() {
        return getAttribute(BIT_STRIKE);
    }

    public Boolean underline2() {
        return getAttribute(BIT_UNDERLINE2);
    }

    public Boolean frame() {
        return getAttribute(BIT_FRAME);
    }

    public Boolean encircle() {
        return getAttribute(BIT_ENCIRCLE);
    }

    public Boolean overline() {
        return getAttribute(BIT_OVERLINE);
    }

    private Boolean getAttribute(int bit) {
        if ((setAttributes & (1 << bit)) == 0) {
            return null;
        }
        return (attributes & (1 << bit)) != 0;
    }

    // =========================================================================
    // Property getters
    // =========================================================================

    public Color getColor() {
        return color;
    }

    public Color getBgcolor() {
        return bgcolor;
    }

    public String getLink() {
        return link;
    }

    public String getLinkId() {
        return linkId;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public boolean isNull() {
        return nullStyle;
    }

    public int getAttributes() {
        return attributes;
    }

    public int getSetAttributes() {
        return setAttributes;
    }

    // =========================================================================
    // Style operations
    // =========================================================================

    /**
     * Combine this style with another style (equivalent to Python's __add__).
     * The other style takes precedence for all explicitly set attributes.
     */
    public Style add(Style other) {
        if (other == null || other.nullStyle) {
            return this;
        }
        if (this.nullStyle) {
            return other.copy();
        }

        // Combine attributes: other's set bits override this one's
        int newSetAttributes = this.setAttributes | other.setAttributes;
        int newAttributes = (this.attributes & ~other.setAttributes) | other.attributes;

        Color newColor = other.color != null ? other.color : this.color;
        Color newBgcolor = other.bgcolor != null ? other.bgcolor : this.bgcolor;
        String newLink = other.link != null ? other.link : this.link;
        String newLinkId = other.link != null ? other.linkId : this.linkId;

        Map<String, Object> newMeta = null;
        if (this.meta != null || other.meta != null) {
            newMeta = new LinkedHashMap<>();
            if (this.meta != null) {
                newMeta.putAll(this.meta);
            }
            if (other.meta != null) {
                newMeta.putAll(other.meta);
            }
        }

        return new Style(newColor, newBgcolor, newAttributes, newSetAttributes,
                         newLink, newLinkId, newMeta, false);
    }

    /**
     * Create a copy of this style.
     */
    public Style copy() {
        if (nullStyle) {
            return NULL_STYLE;
        }
        return new Style(color, bgcolor, attributes, setAttributes,
                         link, linkId, meta, false);
    }

    /**
     * Return a new style with meta and links cleared.
     */
    public Style clearMetaAndLinks() {
        if (nullStyle) {
            return NULL_STYLE;
        }
        return new Style(color, bgcolor, attributes, setAttributes,
                         null, null, null, false);
    }

    /**
     * Return a new style with the link updated.
     */
    public Style updateLink(String newLink) {
        if (nullStyle) {
            return this;
        }
        String newLinkId = newLink != null ? Integer.toHexString(System.identityHashCode(newLink)) : null;
        return new Style(color, bgcolor, attributes, setAttributes,
                         newLink, newLinkId, meta, false);
    }

    /**
     * Return a copy of this style with colors removed.
     */
    public Style withoutColor() {
        if (nullStyle) {
            return NULL_STYLE;
        }
        return new Style(null, null, attributes, setAttributes,
                         link, linkId, meta, false);
    }

    /**
     * Return a style that is just the background color from this style.
     */
    public Style backgroundStyle() {
        if (nullStyle) {
            return NULL_STYLE;
        }
        return new Style(null, bgcolor, 0, 0, null, null, null, false);
    }

    /**
     * Check if this style has a transparent (null/default) background.
     */
    public boolean transparentBackground() {
        return bgcolor == null || bgcolor.isDefault();
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    /**
     * Render text with ANSI escape codes for this style.
     *
     * @param text          the text to render
     * @param colorSystem   the terminal's color system
     * @param legacyWindows whether we are on legacy Windows terminal
     * @return the text wrapped in ANSI escape codes
     */
    public String render(String text, ColorSystem colorSystem, boolean legacyWindows) {
        if (nullStyle || (setAttributes == 0 && color == null && bgcolor == null)) {
            return text;
        }

        StringBuilder ansiBuilder = new StringBuilder();
        renderAnsi(ansiBuilder, colorSystem, legacyWindows);

        if (ansiBuilder.length() == 0) {
            return text;
        }

        String rendered = ESC + ansiBuilder + "m" + text + ANSI_RESET;

        if (link != null && !link.isEmpty()) {
            rendered = "\u001B]8;id=" + linkId + ";" + link + "\u001B\\" + rendered + "\u001B]8;;\u001B\\";
        }

        return rendered;
    }

    /**
     * Render the ANSI escape sequence for this style (without the text).
     */
    public String renderAnsi(ColorSystem colorSystem, boolean legacyWindows) {
        if (nullStyle || (setAttributes == 0 && color == null && bgcolor == null)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        renderAnsi(sb, colorSystem, legacyWindows);
        if (sb.length() == 0) {
            return "";
        }
        return ESC + sb + "m";
    }

    private void renderAnsi(StringBuilder sb, ColorSystem colorSystem, boolean legacyWindows) {
        List<String> codes = new ArrayList<>();

        // Add attribute codes for set attributes that are ON
        for (Map.Entry<Integer, String> entry : STYLE_MAP.entrySet()) {
            int bit = entry.getKey();
            if ((setAttributes & (1 << bit)) != 0 && (attributes & (1 << bit)) != 0) {
                // Skip blink on legacy Windows
                if (legacyWindows && (bit == BIT_BLINK || bit == BIT_BLINK2)) {
                    continue;
                }
                codes.add(entry.getValue());
            }
        }

        // Add color codes
        if (color != null) {
            Color downColor = color;
            if (colorSystem != null) {
                downColor = color.downgrade(colorSystem);
            }
            String[] colorCodes = downColor.getAnsiCodes(true);
            codes.addAll(Arrays.asList(colorCodes));
        }

        if (bgcolor != null) {
            Color downBg = bgcolor;
            if (colorSystem != null) {
                downBg = bgcolor.downgrade(colorSystem);
            }
            String[] bgCodes = downBg.getAnsiCodes(false);
            codes.addAll(Arrays.asList(bgCodes));
        }

        if (!codes.isEmpty()) {
            sb.append(String.join(";", codes));
        }
    }

    // =========================================================================
    // Parse / normalize
    // =========================================================================

    /**
     * Parse a style definition string.
     * Format: "bold red on blue" / "not bold" / "link https://..." etc.
     * Results are cached.
     */
    public static Style parse(String styleDefinition) {
        if (styleDefinition == null || styleDefinition.trim().isEmpty()) {
            return NULL_STYLE;
        }

        // Simple normalization for cache key (avoid calling normalize() which calls parse())
        String normalized = styleDefinition.trim().toLowerCase().replaceAll("\\s+", " ");
        Style cached = PARSE_CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }

        Style result = doParse(normalized);

        if (PARSE_CACHE.size() > PARSE_CACHE_MAX) {
            PARSE_CACHE.clear();
        }
        PARSE_CACHE.put(normalized, result);
        return result;
    }

    private static Style doParse(String styleDefinition) {
        if (styleDefinition == null || styleDefinition.trim().isEmpty()) {
            return NULL_STYLE;
        }

        StyleBuilder builder = new StyleBuilder();
        String[] tokens = styleDefinition.split("\\s+");

        boolean inBackground = false;
        boolean negate = false;

        for (String token : tokens) {
            switch (token) {
                case "" -> {
                    continue;
                }
                case "on" -> {
                    inBackground = true;
                    negate = false;
                    continue;
                }
                case "not" -> {
                    negate = true;
                    continue;
                }
            }

            if (token.startsWith("link:")) {
                builder.link(token.substring(5));
                negate = false;
                continue;
            }

            // Check if it's a style attribute
            Integer bitPos = STYLE_ATTRIBUTES.get(token);
            if (bitPos != null) {
                Boolean value = !negate;
                setBuilderAttribute(builder, bitPos, value);
                negate = false;
                continue;
            }

            // It's a color
            try {
                if (inBackground) {
                    builder.bgcolor(Color.parse(token));
                } else {
                    builder.color(Color.parse(token));
                }
            } catch (Exception e) {
                throw new StyleSyntaxError("Could not parse style '" + styleDefinition + "': " + e.getMessage());
            }
            negate = false;
        }

        return builder.build();
    }

    private static void setBuilderAttribute(StyleBuilder builder, int bitPos, Boolean value) {
        switch (bitPos) {
            case BIT_BOLD:       builder.bold(value); break;
            case BIT_DIM:        builder.dim(value); break;
            case BIT_ITALIC:     builder.italic(value); break;
            case BIT_UNDERLINE:  builder.underline(value); break;
            case BIT_BLINK:      builder.blink(value); break;
            case BIT_BLINK2:     builder.blink2(value); break;
            case BIT_REVERSE:    builder.reverse(value); break;
            case BIT_CONCEAL:    builder.conceal(value); break;
            case BIT_STRIKE:     builder.strike(value); break;
            case BIT_UNDERLINE2: builder.underline2(value); break;
            case BIT_FRAME:      builder.frame(value); break;
            case BIT_ENCIRCLE:   builder.encircle(value); break;
            case BIT_OVERLINE:   builder.overline(value); break;
            default: break;
        }
    }

    /**
     * Normalize a style definition string.
     * Parses the style and converts back to canonical string form.
     * Falls back to simple normalization if parsing fails.
     * Results are cached.
     */
    public static String normalize(String style) {
        if (style == null || style.trim().isEmpty()) {
            return "none";
        }

        String cached = NORMALIZE_CACHE.get(style);
        if (cached != null) {
            return cached;
        }

        String normalized;
        try {
            normalized = Style.parse(style).toString();
        } catch (StyleSyntaxError e) {
            normalized = style.trim().toLowerCase().replaceAll("\\s+", " ");
        }

        if (NORMALIZE_CACHE.size() > PARSE_CACHE_MAX) {
            NORMALIZE_CACHE.clear();
        }
        NORMALIZE_CACHE.put(style, normalized);
        return normalized;
    }

    // =========================================================================
    // toString / style definition reconstruction
    // =========================================================================

    @Override
    public String toString() {
        if (nullStyle) {
            return "Style.null";
        }

        String cached = this.styleDefinition;
        if (cached != null) {
            return cached;
        }

        List<String> parts = new ArrayList<>();

        // Add attribute names
        String[] attrNames = {
            "bold", "dim", "italic", "underline", "blink", "blink2",
            "reverse", "conceal", "strike", "underline2", "frame",
            "encircle", "overline"
        };
        int[] attrBits = {
            BIT_BOLD, BIT_DIM, BIT_ITALIC, BIT_UNDERLINE, BIT_BLINK, BIT_BLINK2,
            BIT_REVERSE, BIT_CONCEAL, BIT_STRIKE, BIT_UNDERLINE2, BIT_FRAME,
            BIT_ENCIRCLE, BIT_OVERLINE
        };

        for (int i = 0; i < attrNames.length; i++) {
            Boolean val = getAttribute(attrBits[i]);
            if (val != null) {
                if (val) {
                    parts.add(attrNames[i]);
                } else {
                    parts.add("not " + attrNames[i]);
                }
            }
        }

        // Add color
        if (color != null) {
            parts.add(color.getName());
        }

        // Add background color
        if (bgcolor != null) {
            parts.add("on");
            parts.add(bgcolor.getName());
        }

        // Add link
        if (link != null) {
            parts.add("link:" + link);
        }

        String result = String.join(" ", parts);
        this.styleDefinition = result;
        return result;
    }

    // =========================================================================
    // HTML style generation
    // =========================================================================

    /**
     * Get the CSS style string for HTML export.
     * Result is cached per theme.
     */
    public String getHtmlStyle(TerminalTheme theme) {
        if (nullStyle) {
            return "";
        }

        // Simple caching using theme identity (since themes are typically constants)
        String cached = this.htmlStyle;
        if (cached != null && theme == TerminalTheme.DEFAULT) {
            return cached;
        }

        List<String> styles = new ArrayList<>();

        if (color != null && !color.isDefault()) {
            ColorTriplet triplet = color.getTruecolor(theme, true);
            styles.add("color:" + triplet.hex());
        }

        if (bgcolor != null && !bgcolor.isDefault()) {
            ColorTriplet triplet = bgcolor.getTruecolor(theme, false);
            styles.add("background-color:" + triplet.hex());
        }

        Boolean b;
        if ((b = bold()) != null && b) styles.add("font-weight:bold");
        if ((b = italic()) != null && b) styles.add("font-style:italic");
        if ((b = underline()) != null && b) styles.add("text-decoration:underline");
        if ((b = strike()) != null && b) {
            String textDecoration = styles.stream()
                .filter(s -> s.startsWith("text-decoration:"))
                .findFirst().orElse(null);
            if (textDecoration != null) {
                styles.remove(textDecoration);
                styles.add(textDecoration + " line-through");
            } else {
                styles.add("text-decoration:line-through");
            }
        }
        if ((b = overline()) != null && b) {
            String textDecoration = styles.stream()
                .filter(s -> s.startsWith("text-decoration:"))
                .findFirst().orElse(null);
            if (textDecoration != null) {
                styles.remove(textDecoration);
                styles.add(textDecoration + " overline");
            } else {
                styles.add("text-decoration:overline");
            }
        }
        if ((b = blink()) != null && b) styles.add("text-decoration:blink");
        if ((b = reverse()) != null && b) {
            // Swap color and background-color
            String colorStyle = null;
            String bgColorStyle = null;
            for (String s : styles) {
                if (s.startsWith("color:")) colorStyle = s;
                if (s.startsWith("background-color:")) bgColorStyle = s;
            }
            if (colorStyle != null) styles.remove(colorStyle);
            if (bgColorStyle != null) styles.remove(bgColorStyle);
            if (colorStyle != null) styles.add(colorStyle.replace("color:", "background-color:"));
            if (bgColorStyle != null) styles.add(bgColorStyle.replace("background-color:", "color:"));
        }
        if ((b = conceal()) != null && b) styles.add("visibility:hidden");
        if ((b = dim()) != null && b) styles.add("opacity:0.5");

        String result = String.join(";", styles);
        if (theme == TerminalTheme.DEFAULT) {
            this.htmlStyle = result;
        }
        return result;
    }

    // =========================================================================
    // equals / hashCode
    // =========================================================================

    @Override
    public int hashCode() {
        if (hash != null) {
            return hash;
        }

        int result = 1;
        result = 31 * result + (nullStyle ? 1 : 0);
        result = 31 * result + attributes;
        result = 31 * result + setAttributes;
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (bgcolor != null ? bgcolor.hashCode() : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (meta != null ? meta.hashCode() : 0);

        hash = result;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Style other)) return false;

        if (this.nullStyle != other.nullStyle) return false;
        if (this.attributes != other.attributes) return false;
        if (this.setAttributes != other.setAttributes) return false;
        if (!Objects.equals(this.color, other.color)) return false;
        if (!Objects.equals(this.bgcolor, other.bgcolor)) return false;
        if (!Objects.equals(this.link, other.link)) return false;
        return Objects.equals(this.meta, other.meta);
    }
}
