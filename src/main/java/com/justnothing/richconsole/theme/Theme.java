package com.justnothing.richconsole.theme;

import java.util.LinkedHashMap;
import java.util.Map;

import com.justnothing.richconsole.style.Style;

/**
 * A container for style information, used by Console.
 * Ported from rich/theme.py.
 */
public class Theme {

    private final Map<String, Style> styles;

    /**
     * Create a new Theme.
     *
     * @param styles  a mapping of style names to styles (may be null);
     *                values may be Style instances or style definition strings
     * @param inherit whether to inherit default styles (true by default)
     */
    public Theme(Map<String, Object> styles, boolean inherit) {
        this.styles = inherit ? DefaultStyles.getStyles() : new LinkedHashMap<>();
        if (styles != null) {
            for (Map.Entry<String, Object> entry : styles.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                Style style;
                if (value instanceof Style) {
                    style = (Style) value;
                } else if (value instanceof String) {
                    style = Style.parse((String) value);
                } else {
                    style = Style.nullStyle();
                }
                this.styles.put(name, style);
            }
        }
    }

    /**
     * Create a new Theme inheriting default styles with no overrides.
     */
    public Theme() {
        this(null, true);
    }

    /**
     * Get a style by name.
     *
     * @param name the style name
     * @return the Style, or null if not found
     */
    public Style getStyle(String name) {
        return styles.get(name);
    }

    /**
     * Get all styles in this theme.
     *
     * @return an unmodifiable view of the styles map
     */
    public Map<String, Style> getStyles() {
        return styles;
    }

    /**
     * Get contents of a config file for this theme.
     *
     * @return INI-style config string
     */
    public String getConfig() {
        StringBuilder sb = new StringBuilder("[styles]\n");
        styles.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Theme(" + styles.size() + " styles)";
    }
}
