package com.justnothing.richconsole.json;

import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.highlighter.JSONHighlighter;
import com.justnothing.richconsole.highlighter.NullHighlighter;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.text.Text;

/**
 * A renderable which pretty prints JSON with syntax highlighting.
 * Ported from rich/json.py.
 */
public class JSON implements RichRenderable {

    private final Text text;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Fluent configuration object for JSON construction.
     * Usage: {@code JSON.of("{\"key\":\"value\"}", cfg -> cfg.indent(2).highlight(false))}
     */
    public static class Config {
        public int indent = 4;
        public boolean highlight = true;

        public Config indent(int indent) { this.indent = indent; return this; }
        public Config highlight(boolean highlight) { this.highlight = highlight; return this; }
    }

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Create a JSON renderable with fluent configuration.
     * <pre>{@code
     * JSON.of("{\"key\":\"value\"}", cfg -> cfg.indent(2).highlight(false))
     * }</pre>
     *
     * @param json        the JSON encoded string (required)
     * @param configurer  a consumer that configures the JSON options
     * @return a new JSON instance
     */
    public static JSON of(String json, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new JSON(json, cfg);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private JSON(String json, Config cfg) {
        this(json, cfg.indent, cfg.highlight);
    }

    /**
     * Create a JSON renderable from a JSON string.
     *
     * @param json      the JSON encoded string
     * @param indent    number of spaces for indentation (default 2)
     * @param highlight whether to enable syntax highlighting
     */
    public JSON(String json, Integer indent, boolean highlight) {
        ObjectMapper mapper = new ObjectMapper();
        Text result;
        try {
            Object data = mapper.readValue(json, Object.class);
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            if (highlight) {
                JSONHighlighter highlighter = new JSONHighlighter();
                result = highlighter.apply(prettyJson);
            } else {
                NullHighlighter highlighter = new NullHighlighter();
                result = highlighter.apply(prettyJson);
            }
            result.setNoWrap(true);
            result.setOverflow(null);
        } catch (JsonProcessingException e) {
            result = new Text(json);
        }
        this.text = result;
    }

    /**
     * Create a JSON renderable from a JSON string with default settings.
     */
    public JSON(String json) {
        this(json, 2, true);
    }

    /**
     * Create a JSON renderable from an arbitrary Java object.
     *
     * @param data      the object to serialize as JSON
     * @param indent    number of spaces for indentation
     * @param highlight whether to enable syntax highlighting
     * @return a JSON renderable
     */
    public static JSON fromData(Object data, Integer indent, boolean highlight) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            return new JSON(json, indent, highlight);
        } catch (JsonProcessingException e) {
            return new JSON("/* serialization error: " + e.getMessage() + " */");
        }
    }

    /**
     * Create a JSON renderable from an arbitrary Java object with default settings.
     */
    public static JSON fromData(Object data) {
        return fromData(data, 2, true);
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        return text.richMeasure(console, options);
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        return text.richConsole(console, options);
    }

    /**
     * Get the underlying Text object.
     */
    public Text getText() {
        return text;
    }
}
