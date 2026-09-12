package com.justnothing.richconsole.syntax;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for looking up syntax themes by name.
 * Mirrors rich/syntax.py RICH_SYNTAX_THEMES plus the default
 * theme name (DEFAULT_THEME = "monokai").
 *
 * <p>In the original rich, vim/solarized/etc. are Pygments styles, not
 * shipped with rich itself, so this port keeps only the themes we can
 * provide without a third-party highlighter.</p>
 */
public final class SyntaxThemes {

    /** Default theme name, matching rich's DEFAULT_THEME. */
    public static final String DEFAULT_THEME = "monokai";

    private static final Map<String, SyntaxTheme> BY_NAME = new HashMap<>();

    static {
        register("monokai", SyntaxTheme.MONOKAI);
        register("ansi_light", SyntaxTheme.ANSI_LIGHT);
        register("ansi_dark", SyntaxTheme.ANSI_DARK);
        // Aliases for convenience
        register("light", SyntaxTheme.ANSI_LIGHT);
        register("dark", SyntaxTheme.ANSI_DARK);
        register("default", SyntaxTheme.MONOKAI);
    }

    private SyntaxThemes() {
    }

    private static void register(String name, SyntaxTheme theme) {
        BY_NAME.put(name.toLowerCase(), theme);
    }

    /**
     * Get a syntax theme by name.
     * Unknown names fall back to the default theme (monokai),
     * mirroring rich's fallback to the "default" Pygments style.
     */
    public static SyntaxTheme get(String name) {
        if (name == null) return SyntaxTheme.MONOKAI;
        SyntaxTheme theme = BY_NAME.get(name.toLowerCase());
        return theme != null ? theme : SyntaxTheme.MONOKAI;
    }
}
