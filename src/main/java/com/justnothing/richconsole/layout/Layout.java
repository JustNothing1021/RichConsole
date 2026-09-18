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
     *
     * @return 这个子树实际用到的下边界（不含）：横向分割时父级靠它把矮的那一列垫高。
     */
    private int buildRenderMap(Console console, ConsoleOptions options,
                               Region region, List<LayoutRenderEntry> entries) {
        if (splits.isEmpty()) {
            // Leaf node — render the content
            List<List<Segment>> lines = renderLeaf(console, options, region);
            entries.add(new LayoutRenderEntry(region, lines));
            return region.y + lines.size();
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
            // 并排：按宽度切分。每一列先渲染到自己的表里，再看谁更高。
            //
            // 不能边算宽度边往 entries 里塞：内容高度是列自己的事，左列矮右列高是常态，
            // 而下面合并行时是按行号对齐的。矮的那一列如果不补够空行，高的那一列多出来的
            // 行就会被当成"这一行只有我"，从 x=0 开始拼 —— 右边面板的底边框会横穿左列，
            // 看上去就像被割成了两半。
            int xOffset = region.x;
            int remainingWidth = region.width;
            List<List<LayoutRenderEntry>> columns = new ArrayList<>(splits.size());
            List<Region> columnRegions = new ArrayList<>(splits.size());
            List<Integer> columnBottoms = new ArrayList<>(splits.size());
            int bottom = region.y;
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
                Region columnRegion = new Region(xOffset, region.y, childWidth, region.height);
                List<LayoutRenderEntry> column = new ArrayList<>();
                int columnBottom = child.buildRenderMap(console, options, columnRegion, column);
                columns.add(column);
                columnRegions.add(columnRegion);
                columnBottoms.add(columnBottom);
                bottom = Math.max(bottom, columnBottom);
                xOffset += childWidth;
            }
            // 按列的顺序放回，空白垫在"自己那一列的最后一行之后"，而不是整张表的末尾：
            // 同一行里各段是按加入顺序拼起来的，垫错位置就等于把这一列挪到别人右边去了。
            for (int i = 0; i < columns.size(); i++) {
                int missing = bottom - columnBottoms.get(i);
                if (missing > 0) {
                    columns.get(i).add(
                            blankEntry(columnRegions.get(i), columnBottoms.get(i), missing));
                }
                entries.addAll(columns.get(i));
            }
            return bottom;
        }

        // Vertical — divide height
        int yOffset = region.y;
        int remainingHeight = region.height;
        int bottom = region.y;
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
            int childBottom = child.buildRenderMap(console, options,
                    new Region(region.x, yOffset, region.width, childHeight), entries);
            bottom = Math.max(bottom, childBottom);
            yOffset += childHeight;
        }
        return bottom;
    }

    /**
     * 一段空白：把"内容不够高"的那一列垫到和同胞列一样高。
     *
     * <p>宽度必须取该列自己的宽度 —— 合并行时后面的列是靠前面列撑出来的偏移量定位的，
     * 少一个空格，右边整块都会往左挪。</p>
     *
     * <p>各行共用同一个空白段是安全的：{@link Segment} 不可变，合并时也只是把引用抄进
     * 新的行里，没人会去改它。</p>
     */
    private static LayoutRenderEntry blankEntry(Region column, int fromRow, int height) {
        List<Segment> blank = Segment.adjustLineLength(new ArrayList<>(), column.width, null, true);
        List<List<Segment>> lines = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            lines.add(blank);
        }
        return new LayoutRenderEntry(new Region(column.x, fromRow, column.width, height), lines);
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

        if (console == null) {
            return new ArrayList<>();
        }

        // Render through Console.render, matching Python's `console.render_lines`
        // (rich/layout.py Layout.render).
        //
        // This used to shortcut to `((RichRenderable) renderable).richConsole(...)`
        // when the content happened to be a RichRenderable. That skips the
        // console's collect/flatten pass, so any renderable that defers its
        // children to the console came out empty — Group is the obvious one:
        // Group.richConsole() just hands back its children without rendering
        // them, so everything was filtered out below and the whole region
        // rendered blank.
        Iterable<Segment> rendered = console.render(renderable, childOptions);

        // Split into lines
        List<List<Segment>> lines = new ArrayList<>();
        for (List<Segment> line : Segment.splitLines(rendered)) {
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
