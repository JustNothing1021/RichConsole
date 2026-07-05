package com.justnothing.richconsole.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.styled.Styled;
import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.cells.Cells;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.text.Text;

/**
 * A table with columns and rows, rendered with box-drawing borders.
 * Ported from rich/table.py Table class.
 */
public class Table implements RichRenderable {

    // Default style names
    private static final String DEFAULT_TABLE_STYLE = "table";
    private static final String DEFAULT_HEADER_STYLE = "table.header";
    private static final int DEFAULT_TABLE_PADDING = 1;

    // =========================================================================
    // Inner class: TableColumn
    // =========================================================================

    public static class TableColumn {
        private Object header;
        private Object footer;
        private Object style;
        private String justify;
        private Integer width;
        private Integer minWidth;
        private Integer maxWidth;
        private Double ratio;
        private boolean noWrap;
        private String overflow;

        public TableColumn(Object header) {
            this.header = header;
            this.footer = null;
            this.style = null;
            this.justify = null;
            this.width = null;
            this.minWidth = null;
            this.maxWidth = null;
            this.ratio = null;
            this.noWrap = false;
            this.overflow = null;
        }

        public TableColumn(Object header, Object style, String justify) {
            this.header = header;
            this.style = style;
            this.justify = justify;
            this.footer = null;
            this.width = null;
            this.minWidth = null;
            this.maxWidth = null;
            this.ratio = null;
            this.noWrap = false;
            this.overflow = null;
        }

        public Object getHeader() {
            return header;
        }

        public Object getFooter() {
            return footer;
        }

        public Object getStyle() {
            return style;
        }

        public String getJustify() {
            return justify;
        }

        public Integer getWidth() {
            return width;
        }

        public Integer getMinWidth() {
            return minWidth;
        }

        public Integer getMaxWidth() {
            return maxWidth;
        }

        public Double getRatio() {
            return ratio;
        }

        public boolean isNoWrap() {
            return noWrap;
        }

        public void setFooter(Object footer) {
            this.footer = footer;
        }

        public void setStyle(Object style) {
            this.style = style;
        }

        public void setJustify(String justify) {
            this.justify = justify;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public void setMinWidth(Integer minWidth) {
            this.minWidth = minWidth;
        }

        public void setMaxWidth(Integer maxWidth) {
            this.maxWidth = maxWidth;
        }

        public void setRatio(Double ratio) {
            this.ratio = ratio;
        }

        public void setNoWrap(boolean noWrap) {
            this.noWrap = noWrap;
        }

        public String getOverflow() {
            return overflow;
        }

        public void setOverflow(String overflow) {
            this.overflow = overflow;
        }
    }

    // =========================================================================
    // Fields
    // =========================================================================

    private final List<TableColumn> columns;
    private final List<List<Object>> rows;
    private Object title;
    private Object caption;
    private Box box;
    private Object style;
    private Object borderStyle;
    private Object headerStyle;
    private Object footerStyle;
    private boolean showHeader;
    private boolean showFooter;
    private boolean showLines;
    private boolean showEdge;
    private boolean expand;
    private int padding;
    private Integer width;
    private boolean collapsePadding;
    private boolean padEdge;

    // =========================================================================
    // Config — fluent configuration for Table construction
    // =========================================================================

    /**
     * Fluent configuration object for Table construction.
     * Usage:
     * {@code new Table(cfg -> cfg.title("Results").expand(true).box(Box.HEAVY))}
     */
    public static class Config {
        public Object title;
        public Object caption;
        public Box box = Box.ROUNDED;
        public Object style = DEFAULT_TABLE_STYLE;
        public Object borderStyle = "";
        public Object headerStyle = DEFAULT_HEADER_STYLE;
        public Object footerStyle;
        public boolean showHeader = true;
        public boolean showFooter = false;
        public boolean showLines = false;
        public boolean showEdge = true;
        public boolean expand = false;
        public int padding = DEFAULT_TABLE_PADDING;
        public Integer width;
        public boolean collapsePadding = false;
        public boolean padEdge = true;

        public Config title(Object title) {
            this.title = title;
            return this;
        }

        public Config caption(Object caption) {
            this.caption = caption;
            return this;
        }

        public Config box(Box box) {
            this.box = box;
            return this;
        }

        public Config style(Object style) {
            this.style = style;
            return this;
        }

        public Config borderStyle(Object borderStyle) {
            this.borderStyle = borderStyle;
            return this;
        }

        public Config headerStyle(Object headerStyle) {
            this.headerStyle = headerStyle;
            return this;
        }

        public Config footerStyle(Object footerStyle) {
            this.footerStyle = footerStyle;
            return this;
        }

        public Config showHeader(boolean showHeader) {
            this.showHeader = showHeader;
            return this;
        }

        public Config showFooter(boolean showFooter) {
            this.showFooter = showFooter;
            return this;
        }

        public Config showLines(boolean showLines) {
            this.showLines = showLines;
            return this;
        }

        public Config showEdge(boolean showEdge) {
            this.showEdge = showEdge;
            return this;
        }

        public Config expand(boolean expand) {
            this.expand = expand;
            return this;
        }

        public Config padding(int padding) {
            this.padding = padding;
            return this;
        }

        public Config width(int width) {
            this.width = width;
            return this;
        }

        public Config collapsePadding(boolean collapsePadding) {
            this.collapsePadding = collapsePadding;
            return this;
        }

        public Config padEdge(boolean padEdge) {
            this.padEdge = padEdge;
            return this;
        }
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    public Table() {
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.title = null;
        this.caption = null;
        this.box = Box.ROUNDED;
        this.style = DEFAULT_TABLE_STYLE;
        this.borderStyle = "";
        this.headerStyle = DEFAULT_HEADER_STYLE;
        this.footerStyle = null;
        this.showHeader = true;
        this.showFooter = false;
        this.showLines = false;
        this.showEdge = true;
        this.expand = false;
        this.padding = DEFAULT_TABLE_PADDING;
        this.width = null;
        this.collapsePadding = false;
        this.padEdge = true;
    }

    /**
     * Construct a Table with a Config consumer for fluent configuration.
     * 
     * <pre>{@code
     * new Table(cfg -> cfg.title("Results").expand(true).box(Box.HEAVY))
     * }</pre>
     */
    public Table(Consumer<Config> configurer) {
        this();
        Config cfg = new Config();
        configurer.accept(cfg);
        this.title = cfg.title;
        this.caption = cfg.caption;
        this.box = cfg.box;
        this.style = cfg.style;
        this.borderStyle = cfg.borderStyle;
        this.headerStyle = cfg.headerStyle;
        this.footerStyle = cfg.footerStyle;
        this.showHeader = cfg.showHeader;
        this.showFooter = cfg.showFooter;
        this.showLines = cfg.showLines;
        this.showEdge = cfg.showEdge;
        this.expand = cfg.expand;
        this.padding = cfg.padding;
        this.width = cfg.width;
        this.collapsePadding = cfg.collapsePadding;
        this.padEdge = cfg.padEdge;
    }

    /**
     * Create a Table with a Config consumer for fluent configuration.
     * <pre>{@code
     * Table.of(cfg -> cfg.title("Results").expand(true).box(Box.HEAVY))
     * }</pre>
     */
    public static Table of(Consumer<Config> configurer) {
        return new Table(configurer);
    }

    /**
     * Create a borderless table with no lines, headers, or footer.
     * Ported from rich/table.py Table.grid classmethod.
     *
     * @param padding padding around cells (default 0)
     * @param expand  whether to expand the table to fit available width
     * @return a new Table configured as a grid (no borders, no headers)
     */
    public static Table grid(int padding, boolean expand) {
        Table table = new Table();
        table.box = null;
        table.showHeader = false;
        table.showFooter = false;
        table.showEdge = false;
        table.showLines = false;
        table.padding = padding;
        table.collapsePadding = true;
        table.padEdge = false;
        table.expand = expand;
        table.style = null;
        table.borderStyle = null;
        return table;
    }

    /**
     * Create a borderless grid table with specified vertical and horizontal
     * padding.
     * Matches Python rich's Table.grid(padding=(vPad, hPad)).
     *
     * @param vPadding vertical padding between rows (not yet supported; reserved for future use)
     * @param hPadding horizontal padding between columns
     * @param expand   whether to expand to full console width
     * @return a new Table configured as a grid
     */
    public static Table grid(int vPadding, int hPadding, boolean expand) {
        Table table = new Table();
        table.box = null;
        table.showHeader = false;
        table.showFooter = false;
        table.showEdge = false;
        table.showLines = false;
        table.padding = hPadding;
        table.collapsePadding = true;
        table.padEdge = false;
        table.expand = expand;
        table.style = null;
        table.borderStyle = null;
        // vPadding is reserved for future use (vertical padding between rows)
        if (vPadding > 0) {
            table.padding = vPadding; // use vPadding as overall padding until separate v/h padding is supported
        }
        return table;
    }

    /**
     * Create a borderless grid table with default padding (0) and no expand.
     */
    public static Table grid() {
        return grid(0, false);
    }

    /**
     * Create a borderless grid table with specified padding.
     */
    public static Table grid(int padding) {
        return grid(padding, false);
    }

    // =========================================================================
    // Column / Row management
    // =========================================================================

    public TableColumn addColumn(Object header) {
        TableColumn col = new TableColumn(header);
        columns.add(col);
        return col;
    }

    public TableColumn addColumn(Object header, Object style, String justify) {
        TableColumn col = new TableColumn(header, style, justify);
        columns.add(col);
        return col;
    }

    public void addRow(Object... cells) {
        List<Object> row = new ArrayList<>();
        Collections.addAll(row, cells);
        rows.add(row);
    }

    // =========================================================================
    // Getters / Setters
    // =========================================================================

    public List<TableColumn> getColumns() {
        return columns;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public Object getTitle() {
        return title;
    }

    public void setTitle(Object title) {
        this.title = title;
    }

    public Object getCaption() {
        return caption;
    }

    public void setCaption(Object caption) {
        this.caption = caption;
    }

    public Box getBox() {
        return box;
    }

    public void setBox(Box box) {
        this.box = box;
    }

    public Object getStyle() {
        return style;
    }

    public void setStyle(Object style) {
        this.style = style;
    }

    public Object getBorderStyle() {
        return borderStyle;
    }

    public void setBorderStyle(Object borderStyle) {
        this.borderStyle = borderStyle;
    }

    public Object getHeaderStyle() {
        return headerStyle;
    }

    public void setHeaderStyle(Object headerStyle) {
        this.headerStyle = headerStyle;
    }

    public Object getFooterStyle() {
        return footerStyle;
    }

    public void setFooterStyle(Object footerStyle) {
        this.footerStyle = footerStyle;
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    public boolean isShowFooter() {
        return showFooter;
    }

    public void setShowFooter(boolean showFooter) {
        this.showFooter = showFooter;
    }

    public boolean isShowLines() {
        return showLines;
    }

    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
    }

    public boolean isShowEdge() {
        return showEdge;
    }

    public void setShowEdge(boolean showEdge) {
        this.showEdge = showEdge;
    }

    public boolean isExpand() {
        return expand;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public boolean isCollapsePadding() {
        return collapsePadding;
    }

    public void setCollapsePadding(boolean collapsePadding) {
        this.collapsePadding = collapsePadding;
    }

    public boolean isPadEdge() {
        return padEdge;
    }

    public void setPadEdge(boolean padEdge) {
        this.padEdge = padEdge;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        // Simplified: measure based on column count and padding
        int minWidth = showEdge ? 2 : 0; // left + right border if showing edges
        int maxWidth = options.getMaxWidth();
        minWidth += columns.size() * (DEFAULT_TABLE_PADDING * 2 + 1); // padding + minimum 1 char content
        return new Measurement(Math.min(minWidth, maxWidth), maxWidth);
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Segment> segments = new ArrayList<>();

        if (columns.isEmpty()) {
            return segments;
        }

        boolean isGrid = (box == null);

        // Resolve styles
        Style borderStyleResolved = borderStyle != null ? console.getStyle(borderStyle) : null;
        Style headerStyleResolved = headerStyle != null ? console.getStyle(headerStyle) : null;
        Style footerStyleResolved = console.getStyle(footerStyle != null ? footerStyle : headerStyle);
        Style tableStyleResolved = style != null ? console.getStyle(style) : null;

        // Calculate available width
        int maxWidth = options.getMaxWidth();
        if (width != null) {
            maxWidth = width;
        }

        // Calculate column widths
        int numCols = columns.size();
        int edgeWidth;
        int innerBorders;
        if (isGrid) {
            // Grid table: no edge borders, padding between columns instead of dividers
            edgeWidth = 0;
            innerBorders = padding * Math.max(0, numCols - 1); // padding acts as separator
        } else {
            edgeWidth = showEdge ? 2 : 0;
            innerBorders = Math.max(0, numCols - 1);
        }
        int availableForColumns = maxWidth - edgeWidth - innerBorders;

        // Use ratio-based column width calculation
        int[] calculatedWidths = calculateColumnWidths(console, options, availableForColumns);

        // Build padded widths list
        List<Integer> paddedWidths = new ArrayList<>();
        for (int w : calculatedWidths) {
            paddedWidths.add(w);
        }

        if (isGrid) {
            // Grid rendering: render each cell into lines, then compose side-by-side
            // Matches Python rich's Table._render() box=None path
            for (List<Object> row : rows) {
                renderGridRow(segments, row, paddedWidths, console, options);
            }
            return segments;
        }

        // ---- Top border ----
        if (showEdge) {
            segments.add(new Segment(box.getTop(paddedWidths), borderStyleResolved));
            segments.add(Segment.line());
        }

        // ---- Header row ----
        if (showHeader) {
            addDataRow(segments, columns, paddedWidths, borderStyleResolved, headerStyleResolved, true, console,
                    options);
            // Header separator
            segments.add(new Segment(box.getRow(paddedWidths, "head", showEdge), borderStyleResolved));
            segments.add(Segment.line());
        }

        // ---- Data rows ----
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            List<Object> row = rows.get(rowIdx);
            List<Object> cellSources = new ArrayList<>();
            for (int colIdx = 0; colIdx < numCols; colIdx++) {
                if (colIdx < row.size()) {
                    cellSources.add(row.get(colIdx));
                } else {
                    cellSources.add("");
                }
            }
            addDataRowFromValues(segments, cellSources, paddedWidths, borderStyleResolved, tableStyleResolved, console,
                    options);

            // Separator between rows (or before footer)
            boolean isLastRow = (rowIdx == rows.size() - 1);
            if (showLines && !isLastRow) {
                segments.add(new Segment(box.getRow(paddedWidths, "row", showEdge), borderStyleResolved));
                segments.add(Segment.line());
            }
        }

        // ---- Footer row ----
        if (showFooter) {
            boolean hasFooters = false;
            for (TableColumn col : columns) {
                if (col.getFooter() != null) {
                    hasFooters = true;
                    break;
                }
            }
            if (hasFooters) {
                segments.add(new Segment(box.getRow(paddedWidths, "foot", showEdge), borderStyleResolved));
                segments.add(Segment.line());
                addDataRow(segments, columns, paddedWidths, borderStyleResolved, footerStyleResolved, false, console,
                        options);
            }
        }

        // ---- Bottom border ----
        if (showEdge) {
            segments.add(new Segment(box.getBottom(paddedWidths), borderStyleResolved));
            segments.add(Segment.line());
        }

        return segments;
    }

    /**
     * Render a row in grid mode (no borders).
     * Matches Python rich's Table._render() box=None path:
     * 1. Render each cell into lines (List<List<Segment>>)
     * 2. Pad each line to the column width
     * 3. Find max height across all cells
     * 4. For each line number, yield all cells' segments for that line, then
     * newline
     */
    private void renderGridRow(List<Segment> segments, List<Object> row,
            List<Integer> paddedWidths,
            Console console, ConsoleOptions options) {
        int numCols = columns.size();

        // Step 1: Render each cell into lines, applying column style
        List<List<List<Segment>>> cellLines = new ArrayList<>();
        int maxHeight = 1;

        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            Object cell = colIdx < row.size() ? row.get(colIdx) : "";
            int colWidth = paddedWidths.get(colIdx);
            TableColumn col = columns.get(colIdx);

            // Resolve column style
            Style colStyle = null;
            if (col.getStyle() != null) {
                colStyle = console.getStyle(col.getStyle());
            }

            // Apply column overflow setting
            String overflow = col.getOverflow();
            ConsoleOptions cellOptions = options.updateWidth(colWidth);
            if (overflow != null) {
                cellOptions = cellOptions.update(null, null, null, null, overflow, null, null, null, null);
            }

            // Render cell into lines
            List<List<Segment>> lines;
            if (cell instanceof RichRenderable) {
                // Apply column style by wrapping with Styled
                Object cellToRender = cell;
                if (colStyle != null && !colStyle.isNull()) {
                    cellToRender = new Styled(cell, colStyle);
                }
                lines = console.renderLines(cellToRender, cellOptions, colStyle, true, false);
            } else {
                // String cell — render through markup parser
                String text = cell != null ? cell.toString() : "";
                if (!text.isEmpty()) {
                    Text rendered = console.renderStr(text, null, null, null, true, true, false);
                    // Apply column style by wrapping with Styled
                    Object cellToRender = rendered;
                    if (colStyle != null && !colStyle.isNull()) {
                        cellToRender = new Styled(rendered, colStyle);
                    }
                    lines = console.renderLines(cellToRender, cellOptions, colStyle, true, false);
                } else {
                    List<Segment> lineSegs = new ArrayList<>();
                    lineSegs.add(new Segment(spaces(colWidth), colStyle));
                    lines = new ArrayList<>();
                    lines.add(lineSegs);
                }
            }

            // Ensure each line is exactly colWidth wide (pad short lines, crop long lines)
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                List<Segment> line = lines.get(lineIdx);
                lines.set(lineIdx, Segment.adjustLineLength(line, colWidth,
                        colStyle != null ? colStyle : Style.nullStyle(), true));
            }

            cellLines.add(lines);
            maxHeight = Math.max(maxHeight, lines.size());
        }

        // Step 2: Compose cells side-by-side, line by line
        for (int lineNo = 0; lineNo < maxHeight; lineNo++) {
            for (int colIdx = 0; colIdx < numCols; colIdx++) {
                // Add inter-column padding (space separator)
                if (colIdx > 0 && padding > 0) {
                    segments.add(new Segment(spaces(padding)));
                }

                List<List<Segment>> lines = cellLines.get(colIdx);
                int colWidth = paddedWidths.get(colIdx);
                TableColumn col = columns.get(colIdx);
                Style colStyle = col.getStyle() != null ? console.getStyle(col.getStyle()) : null;

                if (lineNo < lines.size()) {
                    segments.addAll(lines.get(lineNo));
                } else {
                    // Cell has fewer lines — pad with spaces
                    segments.add(new Segment(spaces(colWidth), colStyle));
                }
            }
            segments.add(Segment.line());
        }
    }

    // =========================================================================
    // Internal rendering helpers
    // =========================================================================

    private void addDataRow(List<Segment> segments, List<TableColumn> cols,
            List<Integer> paddedWidths,
            Style borderStyle, Style cellStyle, boolean isHeader,
            Console console, ConsoleOptions options) {
        // Build cell renderables from column headers or footers
        List<Object> cellValues = new ArrayList<>();
        for (TableColumn col : cols) {
            Object value = isHeader ? col.getHeader() : col.getFooter();
            cellValues.add(value);
        }
        String rowType = isHeader ? Box.HEAD : Box.FOOT;
        renderCells(segments, cellValues, paddedWidths, borderStyle, cellStyle, rowType, console, options);
    }

    private void addDataRowFromValues(List<Segment> segments, List<Object> cellValues,
            List<Integer> paddedWidths,
            Style borderStyle, Style cellStyle,
            Console console, ConsoleOptions options) {
        renderCells(segments, cellValues, paddedWidths, borderStyle, cellStyle, Box.MID, console, options);
    }

    private void renderCells(List<Segment> segments, List<Object> cellValues,
            List<Integer> paddedWidths,
            Style borderStyle, Style cellStyle, String rowType,
            Console console, ConsoleOptions options) {
        // Select edge and divider characters based on row type
        String leftEdge;
        String rightEdge;
        String divider;
        switch (rowType) {
            case Box.HEAD:
                leftEdge = box.headLeft;
                rightEdge = box.headRight;
                divider = box.headVertical;
                break;
            case Box.FOOT:
                leftEdge = box.footLeft;
                rightEdge = box.footRight;
                divider = box.footVertical;
                break;
            default:
                leftEdge = box.midLeft;
                rightEdge = box.midRight;
                divider = box.midVertical;
                break;
        }

        // Step 1: Render each cell into lines, applying column style
        List<List<List<Segment>>> cellLines = new ArrayList<>();
        int maxHeight = 1;

        for (int i = 0; i < cellValues.size(); i++) {
            Object cell = cellValues.get(i);
            int colWidth = paddedWidths.get(i);
            int contentWidth = colWidth - padding * 2;

            // Resolve column style for this cell

            // Render cell into lines using renderLines (supports multi-line cells)
            List<List<Segment>> lines;
            if (cell instanceof RichRenderable) {
                ConsoleOptions cellOptions = options.updateWidth(contentWidth);
                Object cellToRender = cell;
                if (cellStyle != null && !cellStyle.isNull()) {
                    cellToRender = new Styled(cell, cellStyle);
                }
                lines = console.renderLines(cellToRender, cellOptions, cellStyle, true, false);
            } else {
                String text = cell != null ? cell.toString() : "";
                if (!text.isEmpty()) {
                    Text rendered = console.renderStr(text, null, null, null, true, true, false);
                    Object cellToRender = rendered;
                    if (cellStyle != null && !cellStyle.isNull()) {
                        cellToRender = new Styled(rendered, cellStyle);
                    }
                    ConsoleOptions cellOptions = options.updateWidth(contentWidth);
                    lines = console.renderLines(cellToRender, cellOptions, cellStyle, true, false);
                } else {
                    List<Segment> lineSegs = new ArrayList<>();
                    lineSegs.add(new Segment(spaces(contentWidth), cellStyle));
                    lines = new ArrayList<>();
                    lines.add(lineSegs);
                }
            }

            // Ensure each line is exactly contentWidth wide (pad short lines, crop long lines)
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                List<Segment> line = lines.get(lineIdx);
                lines.set(lineIdx, Segment.adjustLineLength(line, contentWidth,
                        cellStyle != null ? cellStyle : Style.nullStyle(), true));
            }

            cellLines.add(lines);
            maxHeight = Math.max(maxHeight, lines.size());
        }

        // Step 2: Compose cells side-by-side, line by line
        for (int lineNo = 0; lineNo < maxHeight; lineNo++) {
            // Left edge
            if (showEdge) {
                segments.add(new Segment(leftEdge, borderStyle));
            }

            for (int i = 0; i < cellValues.size(); i++) {
                int colWidth = paddedWidths.get(i);
                int contentWidth = colWidth - padding * 2;

                // Left padding
                segments.add(new Segment(spaces(padding), cellStyle));

                // Cell content for this line
                List<List<Segment>> lines = cellLines.get(i);
                if (lineNo < lines.size()) {
                    segments.addAll(lines.get(lineNo));
                } else {
                    // Cell has fewer lines — pad with spaces
                    segments.add(new Segment(spaces(contentWidth), cellStyle));
                }

                // Right padding
                segments.add(new Segment(spaces(padding), cellStyle));

                // Divider or right edge
                if (i < cellValues.size() - 1) {
                    segments.add(new Segment(divider, borderStyle));
                } else if (showEdge) {
                    segments.add(new Segment(rightEdge, borderStyle));
                }
            }

            segments.add(Segment.line());
        }
    }

    // =========================================================================
    // Column width calculation
    // =========================================================================

    /**
     * Measure the rendered width of a cell value.
     * For Text objects, uses getPlain(). For other RichRenderables,
     * uses the first line width. Falls back to toString().
     */
    private int measureCellWidth(Object cell) {
        if (cell == null)
            return 0;
        if (cell instanceof Text) {
            return Cells.cellLen(((Text) cell).getPlain());
        }
        if (cell instanceof String) {
            return Cells.cellLen((String) cell);
        }
        // For other types, use toString() as best-effort
        return Cells.cellLen(cell.toString());
    }

    private int[] calculateColumnWidths(Console console, ConsoleOptions options, int availableWidth) {
        // TODO: use console and options for column width measurement
        int numCols = columns.size();
        if (numCols == 0)
            return new int[0];

        int[] widths = new int[numCols];

        // Step 1: Measure each column
        int[] minWidths = new int[numCols];
        int[] maxWidths = new int[numCols];
        double[] ratios = new double[numCols];
        double totalRatio = 0;

        for (int i = 0; i < numCols; i++) {
            TableColumn col = columns.get(i);
            boolean hasExplicitWidth = col.getWidth() != null;
            boolean isFlexible = col.getRatio() != null;

            if (hasExplicitWidth && !isFlexible) {
                // Fixed-width column: width acts as both min and max
                int paddingOverhead = (box != null) ? padding * 2 : 0;
                int explicitWidth = col.getWidth() + paddingOverhead;
                minWidths[i] = explicitWidth;
                maxWidths[i] = explicitWidth;
                ratios[i] = 0;
                continue;
            }

            if (hasExplicitWidth && isFlexible) {
                // Flexible column with explicit width: width acts as minimum
                // Matches Python rich where column.width sets flex_minimum
                int paddingOverhead = (box != null) ? padding * 2 : 0;
                int explicitWidth = col.getWidth() + paddingOverhead;
                minWidths[i] = explicitWidth;
                maxWidths[i] = explicitWidth; // will be expanded by ratio
                ratios[i] = col.getRatio();
                totalRatio += ratios[i];
                continue;
            }

            // Content-based measurement (no explicit width)
            int maxContentWidth = 0;
            int minContentWidth = 1;

            // Measure header
            if (col.getHeader() != null) {
                int headerLen = Cells.cellLen(col.getHeader().toString());
                maxContentWidth = Math.max(maxContentWidth, headerLen);
            }

            // Measure footer
            if (col.getFooter() != null) {
                int footerLen = Cells.cellLen(col.getFooter().toString());
                maxContentWidth = Math.max(maxContentWidth, footerLen);
            }

            // Measure cells from rows
            for (List<Object> row : rows) {
                if (i < row.size()) {
                    Object cell = row.get(i);
                    int cellLen = measureCellWidth(cell);
                    maxContentWidth = Math.max(maxContentWidth, cellLen);
                }
            }

            // Add padding overhead only for bordered tables (grid uses padding as
            // separator)
            if (box != null) {
                int paddingOverhead = padding * 2;
                minContentWidth += paddingOverhead;
                maxContentWidth += paddingOverhead;
            }

            // Apply column constraints
            if (col.getMinWidth() != null) {
                minContentWidth = Math.max(minContentWidth, col.getMinWidth());
            }
            if (col.getMaxWidth() != null) {
                maxContentWidth = Math.min(maxContentWidth, col.getMaxWidth());
            }
            maxContentWidth = Math.max(maxContentWidth, minContentWidth);

            minWidths[i] = minContentWidth;
            maxWidths[i] = maxContentWidth;
            ratios[i] = col.getRatio() != null ? col.getRatio() : 0;
            totalRatio += ratios[i];
        }

        // Step 2: Calculate total minimum and maximum widths
        int totalMin = 0;
        int totalMax = 0;
        for (int i = 0; i < numCols; i++) {
            totalMin += minWidths[i];
            totalMax += maxWidths[i];
        }

        // Step 2: Calculate column widths
        // Matches Python rich's _calculate_column_widths logic:
        // - Non-ratio (fixed) columns get their measured content width
        // - Ratio (flexible) columns share the remaining space by ratio
        if (expand && totalRatio > 0) {
            // Calculate fixed columns' total width
            int fixedTotal = 0;
            for (int i = 0; i < numCols; i++) {
                if (ratios[i] == 0) {
                    fixedTotal += maxWidths[i];
                }
            }

            // Remaining space for flexible columns
            int flexibleWidth = availableWidth - fixedTotal;
            if (flexibleWidth < 0)
                flexibleWidth = 0;

            // Distribute flexible width by ratio (matches Python rich's ratio_distribute)
            int remaining = flexibleWidth;
            int remainingRatio = (int) Math.round(totalRatio);
            for (int i = 0; i < numCols; i++) {
                if (ratios[i] > 0) {
                    if (remainingRatio > 0) {
                        int distributed = (int) Math.ceil(ratios[i] * remaining / remainingRatio);
                        distributed = Math.max(distributed, minWidths[i]);
                        widths[i] = distributed;
                        remaining -= distributed;
                        remainingRatio -= (int) Math.round(ratios[i]);
                    } else {
                        widths[i] = minWidths[i];
                    }
                } else {
                    widths[i] = maxWidths[i];
                }
            }

            // Distribute any remaining pixels due to rounding
            for (int i = 0; i < numCols && remaining > 0; i++) {
                if (ratios[i] > 0) {
                    widths[i]++;
                    remaining--;
                }
            }
        } else if (totalMax <= availableWidth) {
            // Everything fits — use max widths, then distribute extra space by ratio
            int extra = availableWidth - totalMax;
            if (extra > 0 && totalRatio > 0) {
                for (int i = 0; i < numCols; i++) {
                    widths[i] = maxWidths[i];
                    if (ratios[i] > 0) {
                        widths[i] += (int) (extra * ratios[i] / totalRatio);
                    }
                }
                // Adjust for rounding errors
                int usedWidth = 0;
                for (int w : widths)
                    usedWidth += w;
                int remaining = availableWidth - usedWidth;
                for (int i = 0; i < numCols && remaining > 0; i++) {
                    if (ratios[i] > 0) {
                        widths[i]++;
                        remaining--;
                    }
                }
            } else if (expand) {
                // No ratio columns but expand=True: distribute extra evenly
                System.arraycopy(maxWidths, 0, widths, 0, numCols);
                int expandExtra = availableWidth - totalMax;
                for (int i = 0; i < numCols && expandExtra > 0; i = (i + 1) % numCols) {
                    widths[i]++;
                    expandExtra--;
                }
            } else {
                System.arraycopy(maxWidths, 0, widths, 0, numCols);
            }
        } else if (totalMin <= availableWidth) {
            // Need to shrink — reduce from max widths proportionally
            int excess = totalMax - availableWidth;
            for (int i = 0; i < numCols; i++) {
                int shrinkable = maxWidths[i] - minWidths[i];
                widths[i] = maxWidths[i] - (int) ((double) shrinkable * excess / (totalMax - totalMin));
                widths[i] = Math.max(widths[i], minWidths[i]);
            }
            // Adjust for rounding errors
            int usedWidth = 0;
            for (int w : widths)
                usedWidth += w;
            int remaining = availableWidth - usedWidth;
            // Distribute remaining width
            for (int i = 0; remaining > 0; i = (i + 1) % numCols) {
                widths[i]++;
                remaining--;
            }
        } else {
            // Even minimums don't fit — just use minimum widths
            System.arraycopy(minWidths, 0, widths, 0, numCols);
        }

        // Final normalization: ensure total width is within availableWidth
        // Matches Python rich's post-calculation normalization:
        // - If total > availableWidth, shrink from widest columns
        // - If expand and total < availableWidth, distribute extra
        int totalWidth = 0;
        for (int w : widths) totalWidth += w;

        if (totalWidth > availableWidth) {
            // Over-allocated (rounding errors): shrink from widest columns
            int excess = totalWidth - availableWidth;
            while (excess > 0) {
                int widestIdx = 0;
                for (int i = 1; i < numCols; i++) {
                    if (widths[i] > widths[widestIdx]) {
                        widestIdx = i;
                    }
                }
                if (widths[widestIdx] > minWidths[widestIdx]) {
                    widths[widestIdx]--;
                } else {
                    // All at minimum — shrink anyway to fit
                    widths[widestIdx]--;
                }
                excess--;
            }
        } else if (expand && totalWidth < availableWidth) {
            // Under-allocated with expand: distribute extra space evenly
            int extra = availableWidth - totalWidth;
            for (int i = 0; i < numCols && extra > 0; i = (i + 1) % numCols) {
                widths[i]++;
                extra--;
            }
        }

        return widths;
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private static String spaces(int count) {
        if (count <= 0)
            return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

}
