package com.justnothing.richconsole.console;

import com.justnothing.richconsole.theme.Theme;

/**
 * A context manager for temporarily applying a theme to a console.
 * Ported from rich/console.py ThemeContext.
 *
 * <p>Usage:</p>
 * <pre>
 * try (ThemeContext ctx = new ThemeContext(console, myTheme, true)) {
 *     // console uses myTheme here
 * } // theme is popped automatically
 * </pre>
 */
public class ThemeContext implements AutoCloseable {

    private final Object console;
    private final Theme theme;
    private final boolean inherit;

    /**
     * Create a new ThemeContext.
     *
     * @param console the Console instance
     * @param theme   the theme to apply
     * @param inherit whether to inherit the existing theme
     */
    public ThemeContext(Object console, Theme theme, boolean inherit) {
        this.console = console;
        this.theme = theme;
        this.inherit = inherit;
    }

    public ThemeContext(Object console, Theme theme) {
        this(console, theme, true);
    }

    /**
     * Push the theme onto the console's theme stack. Called automatically
     * when used in a try-with-resources block, but can also be called manually.
     */
    public ThemeContext start() {
        invokePushTheme();
        return this;
    }

    /**
     * Pop the theme from the console's theme stack. Called automatically
     * when used in a try-with-resources block.
     */
    @Override
    public void close() {
        invokePopTheme();
    }

    public Theme getTheme() {
        return theme;
    }

    public boolean isInherit() {
        return inherit;
    }

    private void invokePushTheme() {
        try {
            console.getClass().getMethod("pushTheme", Theme.class, boolean.class).invoke(console, theme, inherit);
        } catch (NoSuchMethodException e) {
            try {
                console.getClass().getMethod("pushTheme", Theme.class).invoke(console, theme);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to invoke pushTheme on console", ex);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke pushTheme on console", e);
        }
    }

    private void invokePopTheme() {
        try {
            console.getClass().getMethod("popTheme").invoke(console);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke popTheme on console", e);
        }
    }
}
