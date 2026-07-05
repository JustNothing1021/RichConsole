package com.justnothing.richconsole.syntax;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.justnothing.richconsole.style.Style;

/**
 * Maps token types to Styles for syntax highlighting.
 * Includes built-in ANSI_LIGHT and ANSI_DARK themes.
 * Uses hierarchical fallback: "keyword.declaration" → "keyword" → default.
 */
public class SyntaxTheme {

    private final Map<String, Style> styleMap;
    private final Style defaultStyle;
    private final Style backgroundStyle;

    public SyntaxTheme(Map<String, Style> styleMap, Style defaultStyle) {
        this.styleMap = new HashMap<>(styleMap);
        this.defaultStyle = defaultStyle != null ? defaultStyle : Style.nullStyle();
        this.backgroundStyle = Style.nullStyle();
    }

    public SyntaxTheme(Map<String, Style> styleMap, Style defaultStyle, Style backgroundStyle) {
        this.styleMap = new HashMap<>(styleMap);
        this.defaultStyle = defaultStyle != null ? defaultStyle : Style.nullStyle();
        this.backgroundStyle = backgroundStyle != null ? backgroundStyle : Style.nullStyle();
    }

    public Style getDefaultStyle() {
        return defaultStyle;
    }

    /**
     * Get the background style for the code block.
     */
    public Style getBackgroundStyle() {
        return backgroundStyle;
    }

    /**
     * Get the style for a token type, with hierarchical fallback.
     * "keyword.declaration" falls back to "keyword", then to default.
     */
    public Style getStyleForToken(String tokenType) {
        if (tokenType == null) return defaultStyle;

        // Direct lookup
        Style style = styleMap.get(tokenType);
        if (style != null) return style;

        // Hierarchical fallback: "keyword.declaration" → "keyword"
        String type = tokenType;
        while (type.contains(".")) {
            type = type.substring(0, type.lastIndexOf('.'));
            style = styleMap.get(type);
            if (style != null) return style;
        }

        return defaultStyle;
    }

    // =========================================================================
    // Built-in themes
    // =========================================================================

    /** Light background ANSI theme. */
    public static final SyntaxTheme ANSI_LIGHT;

    /** Dark background ANSI theme. */
    public static final SyntaxTheme ANSI_DARK;

    /** Monokai theme matching Python rich's default colors from Pygments. */
    public static final SyntaxTheme MONOKAI;

    static {
        // ---- ANSI_LIGHT ----
        Map<String, Style> light = new LinkedHashMap<>();
        light.put(SyntaxTokenType.COMMENT, Style.parse("italic black on white"));
        light.put(SyntaxTokenType.COMMENT_SINGLE, Style.parse("italic black on white"));
        light.put(SyntaxTokenType.COMMENT_MULTILINE, Style.parse("italic black on white"));
        light.put(SyntaxTokenType.KEYWORD, Style.parse("bold blue on white"));
        light.put(SyntaxTokenType.KEYWORD_CONSTANT, Style.parse("bold green on white"));
        light.put(SyntaxTokenType.KEYWORD_DECLARATION, Style.parse("bold magenta on white"));
        light.put(SyntaxTokenType.KEYWORD_NAMESPACE, Style.parse("bold magenta on white"));
        light.put(SyntaxTokenType.KEYWORD_TYPE, Style.parse("bold cyan on white"));
        light.put(SyntaxTokenType.NAME, Style.parse("black on white"));
        light.put(SyntaxTokenType.NAME_FUNCTION, Style.parse("blue on white"));
        light.put(SyntaxTokenType.NAME_CLASS, Style.parse("bold blue on white"));
        light.put(SyntaxTokenType.NAME_BUILTIN, Style.parse("cyan on white"));
        light.put(SyntaxTokenType.NAME_EXCEPTION, Style.parse("bold red on white"));
        light.put(SyntaxTokenType.NAME_NAMESPACE, Style.parse("italic cyan on white"));
        light.put(SyntaxTokenType.NAME_DECORATOR, Style.parse("magenta on white"));
        light.put(SyntaxTokenType.NAME_TAG, Style.parse("bold green on white"));
        light.put(SyntaxTokenType.STRING, Style.parse("red on white"));
        light.put(SyntaxTokenType.STRING_AFFIX, Style.parse("bold red on white"));
        light.put(SyntaxTokenType.STRING_DOUBLE, Style.parse("red on white"));
        light.put(SyntaxTokenType.STRING_SINGLE, Style.parse("red on white"));
        light.put(SyntaxTokenType.STRING_DOC, Style.parse("italic red on white"));
        light.put(SyntaxTokenType.STRING_ESCAPE, Style.parse("bold red on white"));
        light.put(SyntaxTokenType.STRING_CHAR, Style.parse("red on white"));
        light.put(SyntaxTokenType.NUMBER, Style.parse("green on white"));
        light.put(SyntaxTokenType.NUMBER_INTEGER, Style.parse("green on white"));
        light.put(SyntaxTokenType.NUMBER_FLOAT, Style.parse("green on white"));
        light.put(SyntaxTokenType.NUMBER_HEX, Style.parse("green on white"));
        light.put(SyntaxTokenType.NUMBER_BIN, Style.parse("green on white"));
        light.put(SyntaxTokenType.NUMBER_OCT, Style.parse("green on white"));
        light.put(SyntaxTokenType.OPERATOR, Style.parse("black on white"));
        light.put(SyntaxTokenType.OPERATOR_WORD, Style.parse("bold blue on white"));
        light.put(SyntaxTokenType.PUNCTUATION, Style.parse("black on white"));
        light.put(SyntaxTokenType.ERROR, Style.parse("bold red on white"));
        ANSI_LIGHT = new SyntaxTheme(light, Style.parse("black on white"), Style.parse("on white"));

        // ---- ANSI_DARK (VSCode-inspired) ----
        Map<String, Style> dark = new LinkedHashMap<>();
        dark.put(SyntaxTokenType.COMMENT, Style.parse("italic dim"));
        dark.put(SyntaxTokenType.COMMENT_SINGLE, Style.parse("italic dim"));
        dark.put(SyntaxTokenType.COMMENT_MULTILINE, Style.parse("italic dim"));
        dark.put(SyntaxTokenType.KEYWORD, Style.parse("bold magenta"));            // control flow
        dark.put(SyntaxTokenType.KEYWORD_CONSTANT, Style.parse("bold cyan"));       // true, false, null
        dark.put(SyntaxTokenType.KEYWORD_DECLARATION, Style.parse("bold blue"));    // modifiers: public, static, etc.
        dark.put(SyntaxTokenType.KEYWORD_NAMESPACE, Style.parse("bold blue"));      // package, import
        dark.put(SyntaxTokenType.KEYWORD_TYPE, Style.parse("green"));               // primitives: int, void, etc.
        dark.put(SyntaxTokenType.KEYWORD_RESERVED, Style.parse("bold red"));
        dark.put(SyntaxTokenType.NAME, Style.parse("cyan"));                        // identifiers
        dark.put(SyntaxTokenType.NAME_FUNCTION, Style.parse("yellow"));             // function calls
        dark.put(SyntaxTokenType.NAME_CLASS, Style.parse("bold green"));            // class names
        dark.put(SyntaxTokenType.NAME_CONSTANT, Style.parse("blue"));               // all-uppercase constants
        dark.put(SyntaxTokenType.NAME_BUILTIN, Style.parse("cyan"));
        dark.put(SyntaxTokenType.NAME_EXCEPTION, Style.parse("bold bright_red"));    // ValueError, TypeError, etc.
        dark.put(SyntaxTokenType.NAME_NAMESPACE, Style.parse("italic cyan"));        // module paths
        dark.put(SyntaxTokenType.NAME_DECORATOR, Style.parse("yellow"));            // @annotations
        dark.put(SyntaxTokenType.NAME_TAG, Style.parse("bold green"));
        dark.put(SyntaxTokenType.STRING, Style.parse("yellow"));
        dark.put(SyntaxTokenType.STRING_AFFIX, Style.parse("bold yellow"));          // r/b/u/f prefix
        dark.put(SyntaxTokenType.STRING_DOUBLE, Style.parse("yellow"));
        dark.put(SyntaxTokenType.STRING_SINGLE, Style.parse("yellow"));
        dark.put(SyntaxTokenType.STRING_DOC, Style.parse("italic yellow"));
        dark.put(SyntaxTokenType.STRING_ESCAPE, Style.parse("bold red"));
        dark.put(SyntaxTokenType.STRING_CHAR, Style.parse("yellow"));
        dark.put(SyntaxTokenType.NUMBER, Style.parse("bright_green"));
        dark.put(SyntaxTokenType.NUMBER_INTEGER, Style.parse("bright_green"));
        dark.put(SyntaxTokenType.NUMBER_FLOAT, Style.parse("bright_green"));
        dark.put(SyntaxTokenType.NUMBER_HEX, Style.parse("bright_green"));
        dark.put(SyntaxTokenType.NUMBER_BIN, Style.parse("bright_green"));
        dark.put(SyntaxTokenType.NUMBER_OCT, Style.parse("bright_green"));
        dark.put(SyntaxTokenType.OPERATOR, Style.parse("white"));
        dark.put(SyntaxTokenType.OPERATOR_WORD, Style.parse("bold magenta"));
        dark.put(SyntaxTokenType.PUNCTUATION, Style.parse("white"));
        dark.put(SyntaxTokenType.ERROR, Style.parse("bold bright_red"));
        ANSI_DARK = new SyntaxTheme(dark, Style.parse("white"), Style.parse("on #1e1e1e"));

        // ---- MONOKAI (Python rich default) ----
        Map<String, Style> monokai = new LinkedHashMap<>();
        monokai.put(SyntaxTokenType.COMMENT, Style.parse("italic #959077 on #272822"));
        monokai.put(SyntaxTokenType.COMMENT_SINGLE, Style.parse("italic #959077 on #272822"));
        monokai.put(SyntaxTokenType.COMMENT_MULTILINE, Style.parse("italic #959077 on #272822"));
        monokai.put(SyntaxTokenType.KEYWORD, Style.parse("bold #66d9ef on #272822"));
        monokai.put(SyntaxTokenType.KEYWORD_CONSTANT, Style.parse("bold #66d9ef on #272822"));
        monokai.put(SyntaxTokenType.KEYWORD_DECLARATION, Style.parse("bold #66d9ef on #272822"));
        monokai.put(SyntaxTokenType.KEYWORD_NAMESPACE, Style.parse("#ff4689 on #272822"));
        monokai.put(SyntaxTokenType.KEYWORD_TYPE, Style.parse("bold #66d9ef on #272822"));
        monokai.put(SyntaxTokenType.KEYWORD_RESERVED, Style.parse("bold #66d9ef on #272822"));
        monokai.put(SyntaxTokenType.NAME, Style.parse("#f8f8f2 on #272822"));
        monokai.put(SyntaxTokenType.NAME_FUNCTION, Style.parse("#a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_CLASS, Style.parse("#a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_BUILTIN, Style.parse("#a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_BUILTIN_PSEUDO, Style.parse("#a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_EXCEPTION, Style.parse("#a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_DECORATOR, Style.parse("#a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_TAG, Style.parse("#ff4689 on #272822"));
        monokai.put(SyntaxTokenType.NAME_NAMESPACE, Style.parse("underline #a6e22e on #272822"));
        monokai.put(SyntaxTokenType.NAME_VARIABLE, Style.parse("#f8f8f2 on #272822"));
        monokai.put(SyntaxTokenType.NAME_CONSTANT, Style.parse("#f8f8f2 on #272822"));
        monokai.put(SyntaxTokenType.NAME_ATTRIBUTE, Style.parse("#f8f8f2 on #272822"));
        monokai.put(SyntaxTokenType.STRING, Style.parse("#e6db74 on #272822"));
        monokai.put(SyntaxTokenType.STRING_DOUBLE, Style.parse("#e6db74 on #272822"));
        monokai.put(SyntaxTokenType.STRING_SINGLE, Style.parse("#e6db74 on #272822"));
        monokai.put(SyntaxTokenType.STRING_DOC, Style.parse("italic #e6db74 on #272822"));
        monokai.put(SyntaxTokenType.STRING_ESCAPE, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.STRING_INTERPOL, Style.parse("#e6db74 on #272822"));
        monokai.put(SyntaxTokenType.STRING_AFFIX, Style.parse("#e6db74 on #272822"));
        monokai.put(SyntaxTokenType.STRING_CHAR, Style.parse("#e6db74 on #272822"));
        monokai.put(SyntaxTokenType.NUMBER, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.NUMBER_INTEGER, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.NUMBER_FLOAT, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.NUMBER_HEX, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.NUMBER_BIN, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.NUMBER_OCT, Style.parse("#ae81ff on #272822"));
        monokai.put(SyntaxTokenType.OPERATOR, Style.parse("#ff4689 on #272822"));
        monokai.put(SyntaxTokenType.OPERATOR_WORD, Style.parse("#ff4689 on #272822"));
        monokai.put(SyntaxTokenType.PUNCTUATION, Style.parse("#f8f8f2 on #272822"));
        monokai.put(SyntaxTokenType.ERROR, Style.parse("#ed007e on #1e0010"));
        MONOKAI = new SyntaxTheme(monokai, Style.parse("#f8f8f2 on #272822"), Style.parse("on #272822"));
    }
}
