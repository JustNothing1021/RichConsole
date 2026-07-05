package com.justnothing.richconsole.theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.justnothing.richconsole.errors.StyleStackError;
import com.justnothing.richconsole.style.Style;

/**
 * A stack of themes.
 * Ported from rich/theme.py ThemeStack.
 */
public class ThemeStack {

    private final List<Map<String, Style>> entries;

    /**
     * Create a ThemeStack with a base theme.
     *
     * @param theme the base theme
     */
    public ThemeStack(Theme theme) {
        this.entries = new ArrayList<>();
        this.entries.add(theme != null ? theme.getStyles() : new LinkedHashMap<>());
    }

    /**
     * Push a theme on top of the stack.
     *
     * @param theme   the theme to push
     * @param inherit whether to inherit styles from the current top of stack
     */
    public void pushTheme(Theme theme, boolean inherit) {
        Map<String, Style> newStyles;
        if (inherit) {
            newStyles = new LinkedHashMap<>(entries.get(entries.size() - 1));
            newStyles.putAll(theme.getStyles());
        } else {
            newStyles = new LinkedHashMap<>(theme.getStyles());
        }
        entries.add(newStyles);
    }

    /**
     * Push a theme on top of the stack, inheriting from the current top.
     *
     * @param theme the theme to push
     */
    public void pushTheme(Theme theme) {
        pushTheme(theme, true);
    }

    /**
     * Pop (and discard) the top-most theme.
     *
     * @throws StyleStackError if the stack would become empty
     */
    public void popTheme() {
        if (entries.size() <= 1) {
            throw new StyleStackError("Unable to pop base theme");
        }
        entries.remove(entries.size() - 1);
    }

    /**
     * Get a style by name from the current top of stack.
     *
     * @param name the style name
     * @return the Style, or null if not found
     */
    public Style get(String name) {
        return entries.get(entries.size() - 1).get(name);
    }

    /**
     * Get the current number of theme layers on the stack.
     */
    public int size() {
        return entries.size();
    }
}
