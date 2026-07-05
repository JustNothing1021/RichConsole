package com.justnothing.richconsole.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.justnothing.richconsole.status.Status;
import com.justnothing.richconsole.align.Align;
import com.justnothing.richconsole.columns.Columns;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.Capture;
import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.containers.Renderables;
import com.justnothing.richconsole.layout.Layout;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.markdown.Markdown;
import com.justnothing.richconsole.padding.Padding;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.pretty.Pretty;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.prompt.Confirm;
import com.justnothing.richconsole.prompt.IntPrompt;
import com.justnothing.richconsole.prompt.Prompt;
import com.justnothing.richconsole.repr.RichRepr;
import com.justnothing.richconsole.rule.Rule;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.segment.SegmentLines;
import com.justnothing.richconsole.segment.Segments;
import com.justnothing.richconsole.spinner.Spinner;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.style.StyleStack;
import com.justnothing.richconsole.syntax.Syntax;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.traceback.Traceback;
import com.justnothing.richconsole.tree.Tree;
import com.justnothing.richconsole.tree.Tree.TreeNode;

/**
 * Demo program for RichConsole.
 * Showcases the main features of the library including animated progress and spinners.
 */
public class RichConsoleDemo {

    public static void main(String[] args) throws InterruptedException {
        Console console = Console.of(cfg -> cfg
                .withColorSystem("auto")
                .withForceTerminal(true)
        );

        // =====================================================================
        // 1. Basic Print & Markup
        // =====================================================================
        console.println("[bold cyan]RichConsole Demo[/]");
        console.println("A Java port of Python's [italic]rich[/italic] library.");
        console.println();

        // =====================================================================
        // 2. Styled Text
        // =====================================================================
        console.println("[bold]Bold[/] [italic]Italic[/] [underline]Underline[/] [strike]Strike[/]");
        console.println("[bold red]Red[/] [green]Green[/] [blue]Blue[/] [yellow]Yellow[/]");
        console.println("[bold on blue]Bold on Blue[/]");
        console.println();

        // =====================================================================
        // 3. Rule (now uses Unicode ─ by default)
        // =====================================================================
        console.println(new Rule("Rules"));
        console.println(Rule.of("Left aligned", cfg -> cfg.style("green").align("left")));
        console.println(Rule.of("Right aligned", cfg -> cfg.style("cyan").align("right")));
        console.println();

        // =====================================================================
        // 4. Panel
        // =====================================================================
        console.println(new Panel("This is a panel with some content inside it.", "Panel"));
        console.println(Panel.of("[cyan]Left aligned title[/]", cfg -> cfg.title("Left").titleAlign("left")));
        console.println(Panel.of("[green]Right aligned title[/]", cfg -> cfg.title("Right").titleAlign("right")));
        console.println();

        // =====================================================================
        // 5. Table
        // =====================================================================
        Table table = new Table();
        table.addColumn("Name");
        table.addColumn("Age");
        table.addColumn("City");
        table.addRow("Alice", "30", "Beijing");
        table.addRow("Bob", "25", "Shanghai");
        table.addRow("Charlie", "35", "Shenzhen");
        console.println(table);
        console.println();

        // =====================================================================
        // 6. Pretty Print
        // =====================================================================
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "RichConsole");
        data.put("version", "0.1.0");
        data.put("language", "Java");
        data.put("features", 10);
        console.println(new Pretty(data));
        console.println();

        List<String> list = new ArrayList<>();
        list.add("terminal");
        list.add("colors");
        list.add("tables");
        list.add("progress");
        console.println(new Pretty(list));
        console.println();

        // =====================================================================
        // 7. Tree
        // =====================================================================
        Tree tree = new Tree("Project");
        TreeNode src = tree.add("src");
        TreeNode main = src.add("main");
        main.add("Console.java");
        main.add("Text.java");
        main.add("Style.java");
        TreeNode test = src.add("test");
        test.add("ConsoleTest.java");
        tree.add("build.gradle");
        tree.add("README.md");
        console.println(tree);
        console.println();

        // =====================================================================
        // 8. Animated Progress Bar (using Live)
        // =====================================================================
        console.println(new Rule("Progress Bar"));

        Progress progress = new Progress(console);
        int task1 = progress.addTask("[red]Downloading", 100);
        int task2 = progress.addTask("[green]Processing", 100);

        try (Live live = new Live(progress, console, 10, false, true)) {
            live.start(true);
            for (int i = 0; i < 100; i++) {
                if (i % 10 == 0) console.println("[green]Downloading " + i + "%...");
                progress.advance(task1, 1.2);
                progress.advance(task2, 0.6);
                live.refresh();
                Thread.sleep(50);
            }
        }
        console.println();

        // =====================================================================
        // 9. Animated Spinner (using Live)
        // =====================================================================
        console.println(new Rule("Spinners"));

        Spinner spinner1 = new Spinner("line", "Loading with line...");
        Spinner spinner2 = new Spinner("dots", "Loading with dots...");
        Spinner spinner3 = new Spinner("bounce", "Bouncing...");

        List<Object> spinnerList = new ArrayList<>();
        spinnerList.add(spinner1);
        spinnerList.add(spinner2);
        spinnerList.add(spinner3);
        try (Live live = new Live(new Group(spinnerList), console, 15, false, true)) {
            live.start(true);
            for (int i = 0; i < 60; i++) {
                Thread.sleep(80);
                live.refresh();
            }
        }
        console.println();

        // =====================================================================
        // 10. Status Demo
        // =====================================================================
        console.println("[bold yellow]Status Demo:[/]");
        try (Status status = console.status("[magenta]Loading data...", "earth", "green", 1.0, 12.5)) {
            status.start();
            Thread.sleep(500);
            console.println("[green]Loading from database...");
            Thread.sleep(500);
            console.println("[green]Loading from cache...");
            Thread.sleep(500);
            console.println("[green]Loading from network...");
            Thread.sleep(500);
            status.update("[bold blue]Processing...");
            Thread.sleep(500);
            console.println("[green]Processing data...");
            Thread.sleep(500);
            console.println("[green]Uploading data...");
            Thread.sleep(500);
            console.println("[green]Data uploaded successfully!");
            status.update("[cyan]Almost done!", "dots", null, null);
            console.println("[green]Finalizing...");
            Thread.sleep(1000);
        }
        console.println("Status cleared automatically after exit!");
        console.println();

        // =====================================================================
        // 11. Capture Demo
        // =====================================================================
        console.println("[bold yellow]Capture Demo:[/]");
        console.println("Content captured during capture() won't display until retrieved:");
        Capture capture = console.capture();
        capture.start();
        console.print("[bold green]Captured text[/] ");
        console.print("[italic cyan]with styles[/]");
        capture.close();
        String captured = capture.get();
        console.println();
        console.print("[bold]Retrieved:[/] ");
        console.printRawNoNewline(captured);
        console.println();
        console.println("(Capture is useful for testing, logging, or post-processing)");
        console.println();

        // =====================================================================
        // 12. Align Demo
        // =====================================================================
        console.println(new Rule("Align"));
        console.println(Align.left("Left aligned", 40, null));
        console.println(Align.center("Center aligned", 40, null));
        console.println(Align.right("Right aligned", 40, null));
        console.println();

        // =====================================================================
        // 13. Columns Demo
        // =====================================================================
        console.println(new Rule("Columns"));
        List<Object> columnItems = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            columnItems.add("[cyan]Item " + i + "[/]");
        }
        console.println(new Columns(columnItems, 2));
        console.println();

        // =====================================================================
        // 14. Layout Demo (side-by-side panels using splitColumn)
        // =====================================================================
        console.println(new Rule("Layout"));
        Layout layoutRoot = new Layout("");
        Layout leftLayout = new Layout(new Panel("[cyan]Left Panel[/]\nContent on the left side", "Left"));
        Layout rightLayout = new Layout(new Panel("[green]Right Panel[/]\nContent on the right side", "Right"));
        leftLayout.setRatio(1);
        rightLayout.setRatio(2);
        layoutRoot.splitColumn(leftLayout, rightLayout);
        console.println(layoutRoot);
        console.println();

        // =====================================================================
        // 15. Padding Demo
        // =====================================================================
        console.println(new Rule("Padding"));
        console.println(new Panel(new Padding("[green]Padded content[/]", 1, 2, 1, 2), "Padding"));
        console.println();

        // =====================================================================
        // 16. Renderables Demo
        // =====================================================================
        console.println(new Rule("Renderables"));
        Renderables renderables = new Renderables();
        renderables.add("[bold red]First[/] renderable");
        renderables.add("[green]Second renderable[/]");
        renderables.add("[blue]Third renderable[/]");
        console.println(renderables);
        console.println();

        // =====================================================================
        // 17. Segments & SegmentLines Demo
        // =====================================================================
        console.println(new Rule("Segments & SegmentLines"));
        // Create segments manually
        List<Segment> manualSegments = new ArrayList<>();
        manualSegments.add(new Segment("Hello ", console.getStyle("bold red")));
        manualSegments.add(new Segment("World!", console.getStyle("bold blue")));
        manualSegments.add(Segment.line());
        manualSegments.add(new Segment("Built from raw ", console.getStyle("italic")));
        manualSegments.add(new Segment("Segment", console.getStyle("bold green")));
        manualSegments.add(new Segment(" objects", console.getStyle("italic")));
        manualSegments.add(Segment.line());
        console.println(new Segments(manualSegments));

        // SegmentLines: pre-split lines
        List<List<Segment>> lines = new ArrayList<>();
        List<Segment> line1 = new ArrayList<>();
        line1.add(new Segment("Line 1: ", console.getStyle("bold")));
        line1.add(new Segment("SegmentLines demo", console.getStyle("cyan")));
        List<Segment> line2 = new ArrayList<>();
        line2.add(new Segment("Line 2: ", console.getStyle("bold")));
        line2.add(new Segment("Multiple lines", console.getStyle("yellow")));
        lines.add(line1);
        lines.add(line2);
        console.println(new SegmentLines(lines, true));
        console.println();

        // =====================================================================
        // 18. StyleStack Demo
        // =====================================================================
        console.println(new Rule("StyleStack"));
        StyleStack styleStack = new StyleStack(Style.parse("white"));
        Style baseStyle = styleStack.current();
        console.print("Base style: ");
        console.printRawNoNewline(baseStyle.render("white text", console.getColorSystemEnum(), console.isLegacyWindows()));
        console.println();
        styleStack.push(Style.parse("bold red"));
        Style pushed1 = styleStack.current();
        console.print("After push(bold red): ");
        console.printRawNoNewline(pushed1.render("bold red text", console.getColorSystemEnum(), console.isLegacyWindows()));
        console.println();
        styleStack.push(Style.parse("on blue"));
        Style pushed2 = styleStack.current();
        console.print("After push(on blue): ");
        console.printRawNoNewline(pushed2.render("bold red on blue text", console.getColorSystemEnum(), console.isLegacyWindows()));
        console.println();
        styleStack.pop();
        Style popped1 = styleStack.current();
        console.print("After pop: ");
        console.printRawNoNewline(popped1.render("back to bold red", console.getColorSystemEnum(), console.isLegacyWindows()));
        console.println();
        styleStack.pop();
        Style popped2 = styleStack.current();
        console.print("After pop again: ");
        console.printRawNoNewline(popped2.render("back to white", console.getColorSystemEnum(), console.isLegacyWindows()));
        console.println();
        console.println();

        // =====================================================================
        // 19. RichRepr Demo
        // =====================================================================
        console.println(new Rule("RichRepr"));
        @RichRepr.RichReprAnnotation
        class DemoPoint {
            public int x;
            public int y;
            public String label;
            DemoPoint(int x, int y, String label) {
                this.x = x; this.y = y; this.label = label;
            }
        }
        DemoPoint point = new DemoPoint(10, 20, "origin");
        console.println("Auto-generated repr: " + RichRepr.autoToString(point));

        @RichRepr.RichReprAnnotation(angular = true)
        class DemoTag {
            public String name;
            public int count;
            DemoTag(String name, int count) {
                this.name = name; this.count = count;
            }
        }
        DemoTag tag = new DemoTag("rich", 42);
        console.println("Angular repr: " + RichRepr.autoToString(tag));
        console.println();

        // =====================================================================
        // 20. Markdown Demo
        // =====================================================================
        console.println(new Rule("Markdown"));

        String mdText = """
                # RichConsole Markdown

                This is a **Markdown** renderer for the _RichConsole_ library,
                ported from Python's `rich` library.

                ## Features

                * Headings with styles (h1-h6)
                * **Bold**, *italic*, and ~~strikethrough~~ text
                * `Inline code` rendering
                * Ordered and unordered lists
                * > Block quotes with visual prefix

                
                ### Code Block

                ```java
                Console console = new Console();
                console.println(new Markdown("# Hello"));
                ```

                ---

                ### Data Table

                | Name    | Type     | Description          |
                |---------|----------|----------------------|
                | Heading | Block    | Section titles       |
                | List    | Block    | Ordered or bullets   |
                | Code    | Inline   | Monospace text       |

                That's all! Visit [RichConsole](https://github.com) for more.
                """;

        console.println(new Markdown(mdText));
        console.println();

        // =====================================================================
        // 21. Syntax Highlighting Demo
        // =====================================================================
        console.println(new Rule("Syntax Highlighting"));

        String javaCode = """
                package com.example;

                import java.util.List;

                /**
                 * A simple demo class.
                 */
                public class Demo {
                    private static final int MAX = 100;

                    public static void main(String[] args) {
                        List<String> names = java.util.Arrays.asList("Alice", "Bob", "Charlie");
                        for (String name : names) {
                            System.out.println(greet(name));
                        }
                    }

                    private static String greet(String name) {
                        return "Hello, " + name + "!";
                    }
                }
                """;

        console.println(Syntax.of(javaCode, cfg -> cfg.lexerName("java").lineNumbers(true)));
        console.println();

        String pythonCode = """
                def fibonacci(n: int) -> list[int]:
                    # Compute Fibonacci sequence.
                    if n <= 0:
                        return []
                    elif n == 1:
                        return [0]

                    fib = [0, 1]
                    for i in range(2, n):
                        fib.append(fib[i-1] + fib[i-2])
                    return fib

                # Print first 10 Fibonacci numbers
                print(fibonacci(10))
                """;

        console.println(Syntax.of(pythonCode, cfg -> cfg.lexerName("python").lineNumbers(true).highlightLines(new java.util.HashSet<>(java.util.Arrays.asList(8)))));
        console.println();

        String jsonCode = """
                {
                    "name": "RichConsole",
                    "version": "0.1.0",
                    "versionCode": 10,
                    "rating": 3.0,
                    "latest": true,
                    "features": ["syntax", "markdown", "traceback"],
                    "dependencies": {
                        "commonmark": "0.22.0",
                        "jline": "3.26.3"
                    }
                }
                """;

        console.println(Syntax.of(jsonCode, cfg -> cfg.lexerName("json").lineNumbers(true)));
        console.println();

        // =====================================================================
        // 22. Traceback Demo
        // =====================================================================
        console.println(new Rule("Traceback"));

        // Create a chained exception for demo
        Throwable cause = new IllegalArgumentException("Invalid argument: null");
        Throwable middle = new IllegalStateException("State is invalid", cause);
        Throwable top = new RuntimeException("Something went wrong", middle);

        console.println(Traceback.fromThrowable(top));
        console.println();

        // Demonstrate with suppressed exceptions
        try {
            try {
                throw new IllegalArgumentException("Primary error");
            } catch (IllegalArgumentException e) {
                e.addSuppressed(new RuntimeException("Suppressed error 1"));
                e.addSuppressed(new RuntimeException("Suppressed error 2"));
                throw e;
            }
        } catch (IllegalArgumentException e) {
            console.println(Traceback.fromThrowable(e));
        }
        console.println();

        // =====================================================================
        // Section 23: Logging
        // =====================================================================
        console.rule("Logging");
        console.log("Hello, World!");
        console.log("[green]I'm green![/green]");
        console.println();

        // =====================================================================
        // Section 24: Convenience APIs & Config Pattern
        // =====================================================================
        console.rule("Convenience APIs & Config Pattern");
        console.rule("Rule via console.rule()");
        console.rule("Left rule", cfg -> cfg.style("green").align("left"));
        console.rule("Right rule", cfg -> cfg.style("cyan").align("right"));

        // Panel with Config pattern
        console.println(Panel.of("Simple content", cfg -> cfg.title("Config Panel").borderStyle("blue")));
        console.println(Panel.fit("Fitted content", cfg -> cfg.title("Fit Panel").borderStyle("green")));

        // Table with Config pattern
        Table configTable = Table.of(cfg -> cfg.title("Config Table").expand(true));
        configTable.addColumn("Feature");
        configTable.addColumn("Style");
        configTable.addRow("Config pattern", "cyan");
        configTable.addRow("Panel.fit()", "green");
        console.println(configTable);

        console.out("Low-level output via console.out()");
        console.println();

        // =====================================================================
        // 25. Prompt Demo (interactive input with live render)
        // =====================================================================
        console.rule("Prompt — Interactive Input");

        String name = Prompt.ask("[bold cyan]What is your name?[/]", console, "World");
        console.println("[green]Hello, " + name + "![/]");

        int age = IntPrompt.askInt("[bold yellow]How old are you?[/]", console, 18);
        console.println("[blue]You are " + age + " years old.[/]");

        boolean proceed = Confirm.askConfirm("[bold magenta]Do you like RichConsole?[/]", console, true);
        if (proceed) {
            console.println("[bold green]Great! Glad you like it![/]");
        } else {
            console.println("[bold red]We'll try harder next time![/]");
        }

        // =====================================================================
        // Final
        // =====================================================================
        console.println(new Rule("Demo Complete"));
    }
}
