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
    private Boolean safeBox; // null = use console default

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
        public Boolean safeBox;

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

        public Config safeBox(boolean safeBox) {
            this.safeBox = safeBox;
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
        this.safeBox = null;
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
        this.safeBox = cfg.safeBox;
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
     * <p>Note: The Table class currently supports a single padding value,
     * applied as horizontal padding between columns. The {@code vPadding}
     * parameter is accepted for API compatibility with Python rich but
     * is not yet used (vertical padding between rows is not supported).
     * Use {@code hPadding} to control horizontal spacing between cells.</p>
     *
     * @param vPadding vertical padding between rows (not yet supported)
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
        // Python rich auto-creates implicit columns when a row has more cells
        // than the current columns, so extra cells are never dropped from the
        // layout (e.g. Scope's "key = value" grid where only the key column is
        // declared explicitly).
        while (row.size() > columns.size()) {
            columns.add(new TableColumn(null));
        }
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

    public Boolean getSafeBox() {
        return safeBox;
    }

    public void setSafeBox(Boolean safeBox) {
        this.safeBox = safeBox;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        // Simplified: measure based on column count and padding
        int minWidth = showEdge ? 2 : 0; // left + right border if showing edges
        int maxWidth = options.getMaxWidth();
        // For grid tables (box=null), padding is used as inter-column separator,
        // so the effective padding per column is 0 (no padding*2). For bordered
        // tables, each column has padding*2 overhead.
        int effectivePadding = box != null ? padding * 2 : 0;
        minWidth += columns.size() * (effectivePadding + 1); // padding + minimum 1 char content
        return new Measurement(Math.min(minWidth, maxWidth), maxWidth);
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Segment> segments = new ArrayList<>();

        // Calculate implicit column count if columns.isEmpty()
        int numCols = columns.size();
        if (numCols == 0) {
            // Grid table without explicit columns: infer from row content
            for (List<Object> row : rows) {
                numCols = Math.max(numCols, row.size());
            }
            if (numCols == 0) {
                return segments; // No columns and no rows with content
            }
        }

        boolean isGrid = (box == null);

        boolean safe = safeBox != null ? safeBox : console.isSafeBox();
        Box resolvedBox = box != null ? box.substitute(options, safe) : null;

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

        // Calculate column widths (handles implicit columns)
        int[] calculatedWidths = calculateColumnWidths(console, options, maxWidth);
        numCols = calculatedWidths.length;  // Update numCols from calculated widths

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
            segments.add(new Segment(resolvedBox.getTop(paddedWidths), borderStyleResolved));
            segments.add(Segment.line());
        }

        // ---- Header row ----
        if (showHeader) {
            addDataRow(segments, columns, paddedWidths, borderStyleResolved, headerStyleResolved, true, console,
                    options, resolvedBox);
            // Header separator
            segments.add(new Segment(resolvedBox.getRow(paddedWidths, "head", showEdge), borderStyleResolved));
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
                    options, resolvedBox);

            // Separator between rows (or before footer)
            boolean isLastRow = (rowIdx == rows.size() - 1);
            if (showLines && !isLastRow) {
                segments.add(new Segment(resolvedBox.getRow(paddedWidths, "row", showEdge), borderStyleResolved));
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
                segments.add(new Segment(resolvedBox.getRow(paddedWidths, "foot", showEdge), borderStyleResolved));
                segments.add(Segment.line());
                addDataRow(segments, columns, paddedWidths, borderStyleResolved, footerStyleResolved, false, console,
                        options, resolvedBox);
            }
        }

        // ---- Bottom border ----
        if (showEdge) {
            segments.add(new Segment(resolvedBox.getBottom(paddedWidths), borderStyleResolved));
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
        // Use paddedWidths.size() which accounts for implicit columns
        int numCols = paddedWidths.size();

        // Step 1: Render each cell into lines, applying column style
        List<List<List<Segment>>> cellLines = new ArrayList<>();
        int maxHeight = 1;

        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            Object cell = colIdx < row.size() ? row.get(colIdx) : "";
            int colWidth = paddedWidths.get(colIdx);
            // For implicit columns, col config is null
            TableColumn col = (colIdx < columns.size()) ? columns.get(colIdx) : null;

            // Resolve column style
            Style colStyle = null;
            if (col != null && col.getStyle() != null) {
                colStyle = console.getStyle(col.getStyle());
            }

            // Apply column settings (justify, noWrap, overflow) matching Python rich's
            // _render() which passes these to options.update()
            String overflow = col != null ? col.getOverflow() : null;
            String justify = col != null ? col.getJustify() : null;
            boolean noWrap = col != null && col.isNoWrap();
            ConsoleOptions cellOptions = options.updateWidth(colWidth);
            // Build non-null args for update() — only pass what's actually set
            String updateOverflow = overflow;
            String updateJustify = justify;
            Boolean updateNoWrap = noWrap ? Boolean.TRUE : null;
            if (updateOverflow != null || updateJustify != null || updateNoWrap != null) {
                cellOptions = cellOptions.update(null, null, null,
                        updateJustify, updateOverflow, updateNoWrap, null, null, null);
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
                TableColumn col = (colIdx < columns.size()) ? columns.get(colIdx) : null;
                Style colStyle = col != null && col.getStyle() != null ? console.getStyle(col.getStyle()) : null;

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
            Console console, ConsoleOptions options, Box resolvedBox) {
        // Build cell renderables from column headers or footers
        List<Object> cellValues = new ArrayList<>();
        for (TableColumn col : cols) {
            Object value = isHeader ? col.getHeader() : col.getFooter();
            cellValues.add(value);
        }
        String rowType = isHeader ? Box.HEAD : Box.FOOT;
        renderCells(segments, cellValues, paddedWidths, borderStyle, cellStyle, rowType, console, options, resolvedBox);
    }

    private void addDataRowFromValues(List<Segment> segments, List<Object> cellValues,
            List<Integer> paddedWidths,
            Style borderStyle, Style cellStyle,
            Console console, ConsoleOptions options, Box resolvedBox) {
        renderCells(segments, cellValues, paddedWidths, borderStyle, cellStyle, Box.MID, console, options, resolvedBox);
    }

    private void renderCells(List<Segment> segments, List<Object> cellValues,
            List<Integer> paddedWidths,
            Style borderStyle, Style cellStyle, String rowType,
            Console console, ConsoleOptions options, Box resolvedBox) {
        // Select edge and divider characters based on row type
        String leftEdge;
        String rightEdge;
        String divider;
        switch (rowType) {
            case Box.HEAD:
                leftEdge = resolvedBox.headLeft;
                rightEdge = resolvedBox.headRight;
                divider = resolvedBox.headVertical;
                break;
            case Box.FOOT:
                leftEdge = resolvedBox.footLeft;
                rightEdge = resolvedBox.footRight;
                divider = resolvedBox.footVertical;
                break;
            default:
                leftEdge = resolvedBox.midLeft;
                rightEdge = resolvedBox.midRight;
                divider = resolvedBox.midVertical;
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
    private Measurement measureCell(Console console, ConsoleOptions options, Object cell) {
        if (cell == null) {
            return new Measurement(0, 0);
        }
        // Use Measurement.get() to properly measure RichRenderable objects
        return Measurement.get(console, options, cell);
    }

    private int[] calculateColumnWidths(Console console, ConsoleOptions options, int availableWidth) {
        int numCols = columns.size();
        if (numCols == 0) {
            // Grid table without explicit columns: infer from row content
            for (List<Object> row : rows) {
                numCols = Math.max(numCols, row.size());
            }
            if (numCols == 0) {
                return new int[0];
            }
        }

        // For grid tables, subtract inter-column padding from available width.
        // renderGridRow adds `padding` spaces between columns, so we need to
        // allocate column widths that fit within the remaining space.
        // This matches Python rich's behavior where padding is accounted for
        // in column width calculation.
        if (box == null && padding > 0 && numCols > 1) {
            int interColumnPadding = padding * (numCols - 1);
            availableWidth = Math.max(0, availableWidth - interColumnPadding);
        }

        int[] widths = new int[numCols];

        // Step 1: Measure each column
        int[] minWidths = new int[numCols];
        int[] maxWidths = new int[numCols];
        double[] ratios = new double[numCols];
        double totalRatio = 0;

        for (int i = 0; i < numCols; i++) {
            // For implicit columns (columns.size() < numCols), use null config
            TableColumn col = (i < columns.size()) ? columns.get(i) : null;
            boolean hasExplicitWidth = col != null && col.getWidth() != null;
            boolean isFlexible = col != null && col.getRatio() != null;

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
            if (col != null && col.getHeader() != null) {
                int headerLen = Cells.cellLen(col.getHeader().toString());
                maxContentWidth = Math.max(maxContentWidth, headerLen);
            }

            // Measure footer
            if (col != null && col.getFooter() != null) {
                int footerLen = Cells.cellLen(col.getFooter().toString());
                maxContentWidth = Math.max(maxContentWidth, footerLen);
            }

            // Measure cells from rows
            for (List<Object> row : rows) {
                if (i < row.size()) {
                    Object cell = row.get(i);
                    Measurement cellMeasure = measureCell(console, options, cell);
                    minContentWidth = Math.max(minContentWidth, cellMeasure.minimum());
                    maxContentWidth = Math.max(maxContentWidth, cellMeasure.maximum());
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
            if (col != null && col.getMinWidth() != null) {
                minContentWidth = Math.max(minContentWidth, col.getMinWidth());
            }
            if (col != null && col.getMaxWidth() != null) {
                maxContentWidth = Math.min(maxContentWidth, col.getMaxWidth());
            }
            maxContentWidth = Math.max(maxContentWidth, minContentWidth);

            minWidths[i] = minContentWidth;
            maxWidths[i] = maxContentWidth;
            // Grid tables (box=null) use ratio=0 by default, matching Python rich.
            // Only columns with explicit ratio (setRatio) get flexible space.
            // Bordered tables also use ratio=0 by default (content-based).
            ratios[i] = (col != null && col.getRatio() != null) ? col.getRatio() : 0;
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
        // - When expand=True and no ratio columns, distribute evenly

        if (expand && totalRatio == 0) {
            // expand=True with no ratio columns: fixed-width columns keep their size,
            // remaining space distributed evenly among flexible columns.
            // This matches Python rich's behavior for grid tables with expand.
            int fixedTotal = 0;
            int flexibleCount = 0;
            for (int i = 0; i < numCols; i++) {
                boolean isFixed = (i < columns.size() && columns.get(i).getWidth() != null);
                if (isFixed) {
                    fixedTotal += minWidths[i];
                } else {
                    flexibleCount++;
                }
            }

            int remaining = availableWidth - fixedTotal;
            if (flexibleCount > 0 && remaining > 0) {
                int perColumn = remaining / flexibleCount;
                int extra = remaining % flexibleCount;
                for (int i = 0; i < numCols; i++) {
                    boolean isFixed = (i < columns.size() && columns.get(i).getWidth() != null);
                    if (isFixed) {
                        widths[i] = minWidths[i];
                    } else {
                        widths[i] = perColumn;
                        if (extra > 0) {
                            widths[i]++;
                            extra--;
                        }
                        widths[i] = Math.max(widths[i], minWidths[i]);
                        widths[i] = Math.min(widths[i], maxWidths[i]);
                    }
                }
            } else {
                // No flexible columns: distribute evenly (capped by maxWidths)
                int perColumn = availableWidth / numCols;
                int extra = availableWidth % numCols;
                for (int i = 0; i < numCols; i++) {
                    widths[i] = perColumn;
                    if (extra > 0) {
                        widths[i]++;
                        extra--;
                    }
                    widths[i] = Math.max(widths[i], minWidths[i]);
                    widths[i] = Math.min(widths[i], maxWidths[i]);
                }
            }
        } else if (totalRatio > 0) {
            // Calculate fixed columns' total width using minWidths (content widths),
            // not maxWidths (which are always fullTerminalWidth due to
            // Measurement.get.withMaximum). This matches Python rich's behavior where
            // non-ratio columns get their content widths and ratio columns share the
            // remaining space.
            int fixedTotal = 0;
            for (int i = 0; i < numCols; i++) {
                if (ratios[i] == 0) {
                    fixedTotal += minWidths[i];
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
                        // Ensure minWidth constraint, but don't exceed availableWidth
                        if (minWidths[i] <= flexibleWidth) {
                            distributed = Math.max(distributed, minWidths[i]);
                        }
                        widths[i] = distributed;
                        remaining -= distributed;
                        remainingRatio -= (int) Math.round(ratios[i]);
                    } else {
                        widths[i] = minWidths[i];
                    }
                } else {
                    widths[i] = minWidths[i];
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
                // expand=false: use content-based max widths, matching Python rich's
                // "widths = [_range.maximum or 1 for _range in width_ranges]"
                System.arraycopy(maxWidths, 0, widths, 0, numCols);
            }
        } else if (totalMin <= availableWidth) {
            // Need to shrink or use content widths
            if (expand) {
                // expand=True: shrink proportionally to fill available width
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
                for (int i = 0; remaining > 0; i = (i + 1) % numCols) {
                    widths[i]++;
                    remaining--;
                }
            } else {
                // expand=false: start from max widths; the final normalization
                // shrinks from the widest columns down to availableWidth if needed.
                System.arraycopy(maxWidths, 0, widths, 0, numCols);
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
