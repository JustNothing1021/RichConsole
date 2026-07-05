package com.justnothing.richconsole.constrain;

import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;

/**
 * Constrain the width and/or height of a renderable.
 * Ported from rich/constrain.py.
 */
public class Constrain implements RichRenderable {

    private final Object renderable;
    private final Integer width;
    private final Integer height;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Fluent configuration object for Constrain construction.
     * Usage: {@code Constrain.of(renderable, cfg -> cfg.width(40).height(10))}
     */
    public static class Config {
        public Integer width;
        public Integer height;

        public Config width(Integer width) { this.width = width; return this; }
        public Config height(Integer height) { this.height = height; return this; }
    }

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Create a Constrain with fluent configuration.
     * <pre>{@code
     * Constrain.of(renderable, cfg -> cfg.width(40).height(10))
     * }</pre>
     *
     * @param renderable  the renderable object (required)
     * @param configurer  a consumer that configures the Constrain options
     * @return a new Constrain instance
     */
    public static Constrain of(Object renderable, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Constrain(renderable, cfg);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private Constrain(Object renderable, Config cfg) {
        this(renderable, cfg.width, cfg.height);
    }

    /**
     * Create a Constrain.
     *
     * @param renderable a renderable object
     * @param width      the maximum width (in characters), or null for no constraint
     * @param height     the maximum height (in lines), or null for no constraint
     */
    public Constrain(Object renderable, Integer width, Integer height) {
        this.renderable = renderable;
        this.width = width;
        this.height = height;
    }

    /**
     * Create a Constrain with only width constraint.
     */
    public Constrain(Object renderable, Integer width) {
        this(renderable, width, null);
    }

    public Object getRenderable() {
        return renderable;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        if (width != null) {
            int constrainedWidth = Math.min(width, options.getMaxWidth());
            ConsoleOptions childOptions = options.updateWidth(constrainedWidth);
            return console.render(renderable, childOptions);
        }
        if (renderable instanceof RichRenderable) {
            return ((RichRenderable) renderable).richConsole(console, options);
        }
        return console.render(renderable, options);
    }

    @Override
    public String toString() {
        return "Constrain(" + renderable + ", width=" + width + ", height=" + height + ")";
    }
}
