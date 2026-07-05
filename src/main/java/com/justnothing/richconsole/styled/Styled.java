package com.justnothing.richconsole.styled;

import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Apply a style to a renderable.
 * Ported from rich/styled.py.
 */
public class Styled implements RichRenderable {

    private final Object renderable;
    private final Object style;

    public static class Config {
        private Object style = null;

        public void style(Object style) { this.style = style; }
    }

    public static Styled of(Object renderable, Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Styled(renderable, config);
    }

    private Styled(Object renderable, Config config) {
        this(renderable, config.style);
    }

    /**
     * Create a Styled renderable.
     *
     * @param renderable any renderable
     * @param style      a style to apply across the entire renderable (Style object or style name String)
     */
    public Styled(Object renderable, Object style) {
        this.renderable = renderable;
        this.style = style;
    }

    public Object getRenderable() {
        return renderable;
    }

    public Object getStyle() {
        return style;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        Style resolvedStyle;
        if (style instanceof Style s) {
            resolvedStyle = s;
        } else if (style instanceof String styleName) {
            resolvedStyle = console.getStyle(styleName);
        } else {
            resolvedStyle = null;
        }
        Iterable<Segment> rendered = console.render(renderable, options);
        return Segment.applyStyle(rendered, resolvedStyle, null);
    }

    @Override
    public String toString() {
        return "Styled(" + renderable + ", " + style + ")";
    }
}
