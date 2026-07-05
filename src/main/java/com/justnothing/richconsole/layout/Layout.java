package com.justnothing.richconsole.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.Segment;

/**
 * A renderable to divide a fixed area into rows or columns.
 * Ported from rich/layout.py.
 *
 * <p>Supports both vertical splits (stacked) and horizontal splits (side-by-side),
 * with ratio-based sizing for child layouts.</p>
 */
public class Layout implements RichRenderable {

    private Object renderable;
    private String name;
    private List<Layout> splits;
    private int ratio = 1;
    private int minimumSize = 1;
    private String direction = "vertical";
    private Object style;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Fluent configuration object for Layout construction.
     * Usage: {@code Layout.of(renderable, cfg -> cfg.name("sidebar"))}
     */
    public static class Config {
        public String name;

        public Config name(String name) { this.name = name; return this; }
    }

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Create a Layout with fluent configuration.
     * <pre>{@code
     * Layout.of(renderable, cfg -> cfg.name("sidebar"))
     * }</pre>
     *
     * @param renderable  the renderable content (required)
     * @param configurer  a consumer that configures the Layout options
     * @return a new Layout instance
     */
    public static Layout of(Object renderable, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Layout(renderable, cfg);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private Layout(Object renderable, Config cfg) {
        this(renderable, cfg.name);
    }

    public Layout(Object renderable) {
        this(renderable, (String) null);
    }

    public Layout(Object renderable, String name) {
        this.renderable = renderable;
        this.name = name;
        this.splits = new ArrayList<>();
    }

    public Object getRenderable() {
        return renderable;
    }

    public void setRenderable(Object renderable) {
        this.renderable = renderable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Layout> getSplits() {
        return splits;
    }

    public int getRatio() {
        return ratio;
    }

    public void setRatio(int ratio) {
        this.ratio = ratio;
    }

    public int getMinimumSize() {
        return minimumSize;
    }

    public void setMinimumSize(int minimumSize) {
        this.minimumSize = minimumSize;
    }

    public String getDirection() {
        return direction;
    }

    public Object getStyle() {
        return style;
    }

    public void setStyle(Object style) {
        this.style = style;
    }

    /**
     * Set child layouts (splits) using the current direction.
     */
    public void split(Layout... layouts) {
        this.splits = new ArrayList<>(Arrays.asList(layouts));
    }

    /**
     * Split horizontally (side by side).
     */
    public void splitColumn(Layout... layouts) {
        this.direction = "horizontal";
        this.splits = new ArrayList<>(Arrays.asList(layouts));
    }

    /**
     * Split vertically (stacked on top of each other).
     */
    public void splitRow(Layout... layouts) {
        this.direction = "vertical";
        this.splits = new ArrayList<>(Arrays.asList(layouts));
    }

    /**
     * Get a named layout, or null if it doesn't exist.
     */
    public Layout get(String layoutName) {
        if (layoutName != null && layoutName.equals(name)) {
            return this;
        }
        for (Layout child : splits) {
            Layout found = child.get(layoutName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Update the renderable content.
     */
    public void update(Object newRenderable) {
        this.renderable = newRenderable;
    }

    // =========================================================================
    // Region class for tracking layout positions
    // =========================================================================

    private static class Region {
        final int x;
        final int y;
        final int width;
        final int height;

        Region(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        int maxWidth = options.getMaxWidth();
        Integer heightObj = options.getHeight();
        int maxHeight = heightObj != null ? heightObj : console.getHeight();

        // Build render map: each leaf Layout → its region and rendered lines
        List<LayoutRenderEntry> renderEntries = new ArrayList<>();
        buildRenderMap(console, options, new Region(0, 0, maxWidth, maxHeight), renderEntries);

        if (renderEntries.isEmpty()) {
            return new ArrayList<>();
        }

        // Calculate the actual max row index needed
        int totalRows = 0;
        for (LayoutRenderEntry entry : renderEntries) {
            int entryEndRow = entry.region.y + entry.lines.size();
            if (entryEndRow > totalRows) {
                totalRows = entryEndRow;
            }
        }

        // Combine rendered lines by extending rows (matching Python's approach)
        @SuppressWarnings("unchecked")
        List<Segment>[] layoutLines = new List[totalRows];
        for (int i = 0; i < totalRows; i++) {
            layoutLines[i] = new ArrayList<>();
        }

        for (LayoutRenderEntry entry : renderEntries) {
            Region region = entry.region;
            List<List<Segment>> lines = entry.lines;
            for (int rowIdx = 0; rowIdx < lines.size(); rowIdx++) {
                int targetRow = region.y + rowIdx;
                if (targetRow >= 0 && targetRow < totalRows) {
                    layoutLines[targetRow].addAll(lines.get(rowIdx));
                }
            }
        }

        List<Segment> segments = new ArrayList<>();
        Segment newLine = Segment.line();
        for (int rowIdx = 0; rowIdx < totalRows; rowIdx++) {
            segments.addAll(layoutLines[rowIdx]);
            segments.add(newLine);
        }

        return segments;
    }

    /**
     * Recursively build a render map for this layout and its children.
     * Each leaf layout is rendered and stored with its region info.
     */
    private void buildRenderMap(Console console, ConsoleOptions options,
                                Region region, List<LayoutRenderEntry> entries) {
        if (splits.isEmpty()) {
            // Leaf node — render the content
            List<List<Segment>> lines = renderLeaf(console, options, region);
            entries.add(new LayoutRenderEntry(region, lines));
            return;
        }

        // Has splits — divide region among children
        int totalRatio = 0;
        for (Layout child : splits) {
            totalRatio += child.ratio;
        }
        if (totalRatio == 0) {
            totalRatio = 1;
        }

        if ("horizontal".equals(direction)) {
            // Side by side — divide width
            int xOffset = region.x;
            int remainingWidth = region.width;
            for (int i = 0; i < splits.size(); i++) {
                Layout child = splits.get(i);
                int childWidth;
                if (i == splits.size() - 1) {
                    // Last child gets all remaining width
                    childWidth = remainingWidth;
                } else {
                    childWidth = Math.max(child.minimumSize,
                            (int) ((double) child.ratio / totalRatio * region.width));
                    remainingWidth -= childWidth;
                }
                child.buildRenderMap(console, options,
                        new Region(xOffset, region.y, childWidth, region.height), entries);
                xOffset += childWidth;
            }
        } else {
            // Vertical — divide height
            int yOffset = region.y;
            int remainingHeight = region.height;
            for (int i = 0; i < splits.size(); i++) {
                Layout child = splits.get(i);
                int childHeight;
                if (i == splits.size() - 1) {
                    childHeight = remainingHeight;
                } else {
                    childHeight = Math.max(child.minimumSize,
                            (int) ((double) child.ratio / totalRatio * region.height));
                    remainingHeight -= childHeight;
                }
                child.buildRenderMap(console, options,
                        new Region(region.x, yOffset, region.width, childHeight), entries);
                yOffset += childHeight;
            }
        }
    }

    /**
     * Render a leaf layout's content constrained to the given region.
     * Returns a list of segment lines, each padded/cropped to region.width.
     */
    private List<List<Segment>> renderLeaf(Console console, ConsoleOptions options, Region region) {
        if (region.width <= 0 || region.height <= 0) {
            return new ArrayList<>();
        }

        ConsoleOptions childOptions = options.updateDimensions(region.width, region.height);

        Iterable<?> rendered;
        if (renderable instanceof RichRenderable) {
            rendered = ((RichRenderable) renderable).richConsole(console, childOptions);
        } else if (console != null) {
            rendered = console.render(renderable, childOptions);
        } else {
            return new ArrayList<>();
        }

        // Convert rendered output to segment list
        List<Segment> allSegments = new ArrayList<>();
        for (Object item : rendered) {
            if (item instanceof Segment) {
                allSegments.add((Segment) item);
            }
        }

        // Split into lines
        List<List<Segment>> lines = new ArrayList<>();
        for (List<Segment> line : Segment.splitLines(allSegments)) {
            lines.add(line);
        }

        // Adjust each line to region.width and crop to region.height
        List<List<Segment>> result = new ArrayList<>();
        int count = 0;
        for (List<Segment> line : lines) {
            if (count >= region.height) break;
            List<Segment> adjusted = Segment.adjustLineLength(line, region.width, null, true);
            result.add(adjusted);
            count++;
        }

        return result;
    }

    /**
     * Entry in the render map: associates a region with rendered lines.
     */
    private static class LayoutRenderEntry {
        final Region region;
        final List<List<Segment>> lines;

        LayoutRenderEntry(Region region, List<List<Segment>> lines) {
            this.region = region;
            this.lines = lines;
        }
    }
}
