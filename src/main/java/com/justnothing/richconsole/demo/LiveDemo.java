package com.justnothing.richconsole.demo;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.status.Status;
import com.justnothing.richconsole.table.Table;

/**
 * Live Demo — 展示实时刷新、Status 和日志。
 */
public class LiveDemo {

    public static void main(String[] args) throws InterruptedException {
        Console console = Console.of(cfg -> {});

        // =====================================================================
        // 1. Logging with timestamps
        // =====================================================================
        console.rule("Logging");
        console.log("Starting application...");
        console.log("Loading [bold]configuration[/bold]");
        console.log("Connecting to database");
        console.log("[green]All systems operational[/green]");
        console.println();

        // =====================================================================
        // 2. Status spinner
        // =====================================================================
        console.rule("Status");
        try (Status status = console.status("Initializing...")) {
            Thread.sleep(1000);
            status.update("[green]Loading modules...[/green]");
            Thread.sleep(1000);
            status.update("[yellow]Compiling sources...[/yellow]");
            Thread.sleep(1000);
            status.update("[cyan]Linking binaries...[/cyan]");
            Thread.sleep(1000);
            status.update("[bold green]Build complete![/bold green]");
            Thread.sleep(500);
        }
        console.println();

        // =====================================================================
        // 3. Log + print interleaving
        // =====================================================================
        console.rule("Log + Print");
        console.log("Before panel");
        console.println(Panel.of("This panel appears between log entries", cfg -> cfg
                .title("Info").borderStyle("green")));
        console.log("After panel");
        console.println();

        // =====================================================================
        // 4. Live table refresh
        // =====================================================================
        console.rule("Live Refresh");
        Table initialStats = Table.of(cfg -> cfg.title("System Stats").expand(false));
        initialStats.addColumn("Metric", "cyan", null);
        initialStats.addColumn("Value", "green", "right");
        initialStats.addRow("CPU", "20%");
        initialStats.addRow("Memory", "30 MB");
        initialStats.addRow("Requests", "0");
        initialStats.addRow("Uptime", "0s");

        try (Live live = new Live(initialStats, console)) {
            live.start();
            for (int i = 1; i <= 10; i++) {
                Table stats = Table.of(cfg -> cfg.title("System Stats").expand(false));
                stats.addColumn("Metric", "cyan", null);
                stats.addColumn("Value", "green", "right");
                stats.addRow("CPU", (20 + i * 5) + "%");
                stats.addRow("Memory", (30 + i * 3) + " MB");
                stats.addRow("Requests", String.valueOf(i * 100));
                stats.addRow("Uptime", i + "s");
                live.update(stats);
                Thread.sleep(300);
            }
        }
        console.println();

        // =====================================================================
        // 5. Convenience APIs
        // =====================================================================
        console.rule("Convenience APIs");
        console.rule("Styled rule", cfg -> cfg.style("red").align("center"));
        console.out("Output via console.out()");
        console.println();
        console.log("Timestamped log entry");
        console.println();

        // =====================================================================
        // 6. Exception display
        // =====================================================================
        console.rule("Exception Display");
        try {
            int result = 10 / 0;
        } catch (ArithmeticException e) {
            console.printException(e);
        }
        console.println();
    }
}
