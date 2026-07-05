package com.justnothing.richconsole.demo;

import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.layout.Layout;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.table.Table;

/**
 * Layout & Panel Demo — 展示布局组合和 Panel 的各种用法。
 */
public class LayoutPanelDemo {

    public static void main(String[] args) {
        Console console = Console.of(cfg -> {});

        // =====================================================================
        // 1. Panel basics
        // =====================================================================
        console.rule("Panel Basics");
        console.println(new Panel("A simple panel"));
        console.println(new Panel("Panel with title", "My Title"));
        console.println(Panel.of("Styled panel", cfg -> cfg
                .title("Styled").borderStyle("red").expand(false)));
        console.println();

        // =====================================================================
        // 2. Panel.fit() — auto-fit to content width
        // =====================================================================
        console.rule("Panel.fit()");
        console.println(Panel.fit("This panel fits its content width", cfg -> cfg.title("Fit Panel")));
        console.println(Panel.fit("Short text", "Compact"));
        console.println();

        // =====================================================================
        // 3. Panel with different border styles
        // =====================================================================
        console.rule("Panel Border Styles");
        String[] borderStyles = {"red", "green", "blue", "yellow", "cyan", "magenta"};
        for (String style : borderStyles) {
            console.println(Panel.fit("Panel with " + style + " border",
                    cfg -> cfg.borderStyle(style)));
        }
        console.println();

        // =====================================================================
        // 4. Panel with subtitle and alignment
        // =====================================================================
        console.rule("Panel Title & Subtitle");
        console.println(Panel.of("Content here", cfg -> cfg
                .title("Title").subtitle("Subtitle").borderStyle("cyan")));
        console.println(Panel.of("Left title", cfg -> cfg
                .title("Left Title").titleAlign("left").borderStyle("green")));
        console.println(Panel.of("Right title", cfg -> cfg
                .title("Right Title").titleAlign("right").borderStyle("yellow")));
        console.println();

        // =====================================================================
        // 5. Nested panels
        // =====================================================================
        console.rule("Nested Panels");
        Panel inner = Panel.of("Inner content", cfg -> cfg
                .title("Inner").borderStyle("green").expand(false));
        console.println(Panel.of(inner, cfg -> cfg
                .title("Outer").borderStyle("blue")));
        console.println();

        // =====================================================================
        // 6. Layout — side-by-side
        // =====================================================================
        console.rule("Layout");
        Layout left = new Layout("Left panel\nwith some\ncontent", "Left");
        Layout right = new Layout("Right panel\nwith other\ncontent", "Right");

        Layout layout = new Layout(new Group(
                java.util.Arrays.asList(left, right)));
        console.println(layout);
        console.println();

        // =====================================================================
        // 7. Panel with Table inside
        // =====================================================================
        console.rule("Panel with Table");
        Table tableInPanel = Table.of(cfg -> cfg.expand(false));
        tableInPanel.addColumn("Key");
        tableInPanel.addColumn("Value");
        tableInPanel.addRow("CPU", "85%");
        tableInPanel.addRow("Memory", "4.2 GB");
        tableInPanel.addRow("Disk", "120 GB");
        console.println(Panel.fit(tableInPanel, "System Info"));
        console.println();
    }
}
