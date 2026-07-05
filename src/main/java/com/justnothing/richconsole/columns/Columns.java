package com.justnothing.richconsole.columns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.table.Table.TableColumn;

/**
 * Renders renderables in columns, arranged side by side.
 * Ported from rich/columns.py Columns class.
 *
 * <p>Uses a Table.grid internally to lay out items in columns.</p>
 */
public class Columns implements RichRenderable {

    private static final int DEFAULT_PADDING = 1;

    private final List<Object> renderables;
    private final int padding;
    private final Integer width;
    private final boolean expand;
    private final boolean equal;
    private final String align;

    // =========================================================================
    // Config inner class
    // =========================================================================

    public static class Config {
        public int padding = 1;
        public Integer width = null;
        public boolean expand = false;
        public boolean equal = false;
        public String align = "left";
    }

    public static Columns of(List<?> renderables, Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Columns(renderables, config);
    }

    private Columns(List<?> renderables, Config config) {
        this(renderables, config.padding, config.width, config.expand, config.equal, config.align);
    }

    public Columns(List<?> renderables, int padding, Integer width,
                   boolean expand, boolean equal, String align) {
        this.renderables = renderables != null
                ? new ArrayList<>(renderables)
                : new ArrayList<>();
        this.padding = padding;
        this.width = width;
        this.expand = expand;
        this.equal = equal;
        this.align = align != null ? align : "left";
    }

    public Columns(List<?> renderables, int padding) {
        this(renderables, padding, null, false, false, "left");
    }

    public Columns(List<?> renderables) {
        this(renderables, DEFAULT_PADDING, null, false, false, "left");
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        if (renderables.isEmpty()) {
            return Collections.emptyList();
        }

        // Convert strings to RichRenderables (matching Python: render_str if str)
        List<Object> resolvedRenderables = new ArrayList<>();
        for (Object renderable : renderables) {
            if (renderable instanceof String) {
                resolvedRenderables.add(console.renderStr((String) renderable));
            } else {
                resolvedRenderables.add(renderable);
            }
        }

        int maxWidth = options.getMaxWidth();

        // Measure each renderable's width
        List<Integer> renderableWidths = new ArrayList<>();
        for (Object renderable : resolvedRenderables) {
            Measurement m = Measurement.get(console, options, renderable);
            renderableWidths.add(m.maximum());
        }

        if (equal) {
            int maxW = 0;
            for (int w : renderableWidths) {
                maxW = Math.max(maxW, w);
            }
            for (int i = 0; i < renderableWidths.size(); i++) {
                renderableWidths.set(i, maxW);
            }
        }

        // Determine column count
        int columnCount = resolvedRenderables.size();
        if (width != null) {
            columnCount = Math.max(1, maxWidth / (width + padding));
        } else {
            // Reduce column count until all columns fit in maxWidth
            while (columnCount > 1) {
                int[] colWidths = new int[columnCount];
                boolean fits = true;
                for (int i = 0; i < resolvedRenderables.size(); i++) {
                    int colNo = i % columnCount;
                    colWidths[colNo] = Math.max(colWidths[colNo], renderableWidths.get(i));
                    int totalWidth = 0;
                    for (int w : colWidths) totalWidth += w;
                    totalWidth += padding * (columnCount - 1);
                    if (totalWidth > maxWidth) {
                        columnCount = colNo;
                        fits = false;
                        break;
                    }
                }
                if (fits) break;
            }
        }

        columnCount = Math.max(1, columnCount);

        // Build a grid Table
        Table grid = Table.grid(padding);
        grid.setExpand(expand);

        for (int col = 0; col < columnCount; col++) {
            TableColumn tableCol = grid.addColumn("");
            if (width != null) {
                tableCol.setWidth(width);
            }
        }

        // Add rows
        for (int start = 0; start < resolvedRenderables.size(); start += columnCount) {
            List<Object> row = new ArrayList<>();
            for (int col = 0; col < columnCount; col++) {
                int idx = start + col;
                if (idx < resolvedRenderables.size()) {
                    row.add(resolvedRenderables.get(idx));
                } else {
                    row.add("");
                }
            }
            grid.addRow(row.toArray());
        }

        return grid.richConsole(console, options);
    }

    @Override
    public String toString() {
        return "Columns(renderables=" + renderables.size()
                + ", padding=" + padding
                + ", expand=" + expand
                + ", equal=" + equal
                + ", align=" + align + ")";
    }
}
