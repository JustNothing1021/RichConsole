package com.justnothing.richconsole.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;

/**
 * A container for multiple renderables.
 * Ported from rich/containers.py Renderables.
 */
public class Renderables implements RichRenderable {

    private final List<Object> renderables;

    public Renderables(List<Object> renderables) {
        this.renderables = renderables != null
                ? new ArrayList<>(renderables)
                : new ArrayList<>();
    }

    public Renderables() {
        this.renderables = new ArrayList<>();
    }

    public List<Object> getRenderables() {
        return Collections.unmodifiableList(renderables);
    }

    public void add(Object renderable) {
        renderables.add(renderable);
    }

    public int size() {
        return renderables.size();
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        // Measure the widest renderable among the contained items
        int maxWidth = options.getMaxWidth();
        int minWidth = 0;
        for (Object renderable : renderables) {
            Measurement m = Measurement.get(console, options, renderable);
            minWidth = Math.max(minWidth, m.minimum());
            // Don't accumulate maxWidth — just use the console width
            // Each renderable can be as wide as the console allows
        }
        return new Measurement(minWidth, maxWidth);
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Object> result = new ArrayList<>();
        for (Object renderable : renderables) {
            // Convert strings to RichRenderables (matching Python: render_str if str)
            Object resolved = renderable;
            if (renderable instanceof String && console != null) {
                resolved = console.renderStr((String) renderable);
            }
            if (resolved instanceof RichRenderable) {
                Iterable<?> rendered = ((RichRenderable) resolved).richConsole(console, options);
                for (Object item : rendered) {
                    result.add(item);
                }
            } else {
                result.add(renderable);
            }
        }
        return result;
    }
}
