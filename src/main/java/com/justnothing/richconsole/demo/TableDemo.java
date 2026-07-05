package com.justnothing.richconsole.demo;

import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.table.Table;

/**
 * Table Demo — 展示各种表格样式和功能。
 * Ported from rich's __main__.py test card table section.
 */
public class TableDemo {

    public static void main(String[] args) {
        Console console = Console.of(cfg -> {});

        // =====================================================================
        // 1. Basic Table with borders
        // =====================================================================
        console.rule("Basic Table");
        Table basic = Table.of(cfg -> cfg.title("Star Wars Movies").expand(true));
        basic.addColumn("[green]Date", "green", null).setNoWrap(true);
        basic.addColumn("[blue]Title", "blue", null);
        basic.addColumn("[cyan]Budget", "cyan", "right").setNoWrap(true);
        basic.addColumn("[magenta]Box Office", "magenta", "right").setNoWrap(true);
        basic.addRow("Dec 20, 2019", "Star Wars: The Rise of Skywalker", "$275,000,000", "$375,126,118");
        basic.addRow("May 25, 2018", "[b]Solo[/]: A Star Wars Story", "$275,000,000", "$393,151,347");
        basic.addRow("Dec 15, 2017", "Star Wars Ep. VIII: The Last Jedi", "$262,000,000", "[bold]$1,332,539,889[/bold]");
        basic.addRow("May 19, 1999", "Star Wars Ep. [b]I[/b]: [i]The phantom Menace", "$115,000,000", "$1,027,044,677");
        console.println(basic);
        console.println();

        // =====================================================================
        // 2. Grid Table (no borders)
        // =====================================================================
        console.rule("Grid Table");
        Table grid = Table.grid(0, 1, false);
        Table.TableColumn keyCol = grid.addColumn("Key", "cyan", null);
        keyCol.setNoWrap(true);
        Table.TableColumn valCol = grid.addColumn("Value", "green", null);
        valCol.setRatio(1.0);
        grid.addRow("Name", "RichConsole");
        grid.addRow("Version", "1.0.0");
        grid.addRow("Language", "Java 17+");
        grid.addRow("License", "MIT");
        console.println(grid);
        console.println();

        // =====================================================================
        // 3. Different Box Styles
        // =====================================================================
        console.rule("Box Styles");
        Box[] boxes = {Box.ROUNDED, Box.SQUARE, Box.HEAVY, Box.SIMPLE, Box.MINIMAL, Box.ASCII};
        for (Box b : boxes) {
            Table t = Table.of(cfg -> cfg.box(b).expand(false).padding(1));
            t.addColumn("Name");
            t.addColumn("Value");
            t.addRow("style", b.getClass().getSimpleName());
            console.println(Panel.of(t, cfg -> cfg.title(b.toString()).box(b).expand(false)));
        }
        console.println();

        // =====================================================================
        // 4. Ratio columns
        // =====================================================================
        console.rule("Ratio Columns");
        Table ratio = Table.of(cfg -> cfg.expand(true));
        Table.TableColumn col1 = ratio.addColumn("1/4", "cyan", null);
        col1.setRatio(1.0);
        Table.TableColumn col2 = ratio.addColumn("3/4", "green", null);
        col2.setRatio(3.0);
        ratio.addRow("First column", "Second column takes 3x more space");
        ratio.addRow("1", "2 3 4");
        console.println(ratio);
        console.println();

        // =====================================================================
        // 5. Table with Config pattern
        // =====================================================================
        console.rule("Config Pattern", cfg -> cfg.style("blue"));
        Table configTable = Table.of(cfg -> cfg
                .title("Styled with Config")
                .borderStyle("red")
                .headerStyle("bold yellow")
                .expand(true)
                .showLines(true));
        configTable.addColumn("Feature", "cyan", null);
        configTable.addColumn("Description", "green", null);
        configTable.addRow("Config pattern", "Fluent API for table configuration");
        configTable.addRow("Panel.fit()", "Auto-fit panel to content width");
        configTable.addRow("Rule config", "Configure rule style and alignment");
        console.println(configTable);
        console.println();

        // =====================================================================
        // 6. Text alignment in table cells
        // =====================================================================
        console.rule("Text Alignment");
        Table align = Table.of(cfg -> cfg.expand(true));
        align.addColumn("Left", "green", "left");
        align.addColumn("Center", "yellow", "center");
        align.addColumn("Right", "blue", "right");
        String lorem = "Lorem ipsum dolor sit amet";
        align.addRow(lorem, lorem, lorem);
        align.addRow("Left", "Center", "Right");
        console.println(align);
        console.println();
    }
}
