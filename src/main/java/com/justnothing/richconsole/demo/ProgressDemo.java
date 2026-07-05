package com.justnothing.richconsole.demo;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.status.Status;

/**
 * Progress Demo — 展示进度条、Spinner 和 Status。
 */
public class ProgressDemo {

    public static void main(String[] args) throws InterruptedException {
        Console console = Console.of(cfg -> {});

        // =====================================================================
        // 1. Single progress bar
        // =====================================================================
        console.rule("Single Progress Bar");
        try (Progress progress = console.progress()) {
            int task = progress.addTask("Processing", 100);
            progress.start();
            while (!progress.getTask(task).isFinished()) {
                Thread.sleep(60);
                progress.advance(task, 1);
            }
        }
        console.println();

        // =====================================================================
        // 2. Multiple concurrent progress bars
        // =====================================================================
        console.rule("Multiple Progress Bars");
        try (Progress progress = console.progress()) {
            int task1 = progress.addTask("Downloading", 200);
            int task2 = progress.addTask("Extracting", 150);
            int task3 = progress.addTask("Installing", 100);
            progress.start();
            while (!progress.isFinished()) {
                Thread.sleep(40);
                if (!progress.getTask(task1).isFinished()) progress.advance(task1, 2);
                if (!progress.getTask(task2).isFinished()) progress.advance(task2, 1.5);
                if (!progress.getTask(task3).isFinished()) progress.advance(task3, 1);
            }
        }
        console.println();

        // =====================================================================
        // 3. Indeterminate progress (total = 0)
        // =====================================================================
        console.rule("Indeterminate Progress");
        try (Progress progress = console.progress()) {
            int task = progress.addTask("Waiting for response...", 0);
            progress.start();
            Thread.sleep(3000);
            progress.update(task, 100.0, 100.0, null, null, null, false, null);
        }
        console.println();

        // =====================================================================
        // 4. Status spinner
        // =====================================================================
        console.rule("Status Spinner");
        try (Status status = console.status("Loading database...")) {
            Thread.sleep(1500);
            status.update("Compiling shaders...");
            Thread.sleep(1500);
            status.update("Ready!");
            Thread.sleep(500);
        }
        console.println();

        // =====================================================================
        // 5. Progress with log messages
        // =====================================================================
        console.rule("Progress with Logging");
        try (Progress progress = console.progress()) {
            int task = progress.addTask("Deploying", 10);
            progress.start();
            String[] steps = {"Build", "Test", "Package", "Upload", "Verify", "Configure", "Migrate", "Seed", "Restart", "Done!"};
            for (int i = 0; i < steps.length; i++) {
                progress.advance(task, 1);
                console.log("[green]" + steps[i] + "[/green] completed");
                Thread.sleep(300);
            }
        }
        console.println();
    }
}
