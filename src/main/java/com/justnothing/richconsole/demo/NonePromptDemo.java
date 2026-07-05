package com.justnothing.richconsole.demo;

import java.util.List;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.noneprompt.CancelledException;
import com.justnothing.richconsole.noneprompt.CheckboxPrompt;
import com.justnothing.richconsole.noneprompt.Choice;
import com.justnothing.richconsole.noneprompt.ConfirmPrompt;
import com.justnothing.richconsole.noneprompt.InputPrompt;
import com.justnothing.richconsole.noneprompt.ListPrompt;

/**
 * Demo for noneprompt interactive prompts.
 */
public class NonePromptDemo {

    public static void main(String[] args) {
        Console console = Console.of(cfg -> cfg
                .withForceTerminal(true)
                .withColorSystem("truecolor")
        );

        console.println("[bold cyan]NonePrompt Demo[/]");
        console.println("Interactive prompts ported from Python noneprompt");
        console.println();

        // 1. ListPrompt - Single select
        console.println("[bold]1. ListPrompt[/] — Single select with filter");
        try {
            List<Choice<String>> fruits = java.util.Arrays.asList(
                    new Choice<>("Apple", "apple"),
                    new Choice<>("Banana", "banana"),
                    new Choice<>("Cherry", "cherry"),
                    new Choice<>("Dragon Fruit", "dragon_fruit"),
                    new Choice<>("Elderberry", "elderberry"),
                    new Choice<>("Fig", "fig"),
                    new Choice<>("Grape", "grape")
            );
            Choice<String> selected = ListPrompt.ask("Which fruit do you like?", fruits, console);
            console.println("  You selected: [green]" + selected.name() + "[/] (data: " + selected.data() + ")");
        } catch (CancelledException e) {
            console.println("  [dim]Cancelled[/]");
        }
        console.println();

        // 2. CheckboxPrompt - Multi select
        console.println("[bold]2. CheckboxPrompt[/] — Multi select with checkboxes");
        try {
            List<Choice<String>> languages = java.util.Arrays.asList(
                    new Choice<>("Java", "java"),
                    new Choice<>("Python", "python"),
                    new Choice<>("Rust", "rust"),
                    new Choice<>("Go", "go"),
                    new Choice<>("TypeScript", "typescript"),
                    new Choice<>("C++", "cpp")
            );
            List<Choice<String>> selected = CheckboxPrompt.ask("Which languages do you use?", languages, console);
            console.println("  You selected: [green]" +
                    String.join("[/], [green]", selected.stream().map(Choice::name).collect(java.util.stream.Collectors.toList())) + "[/]");
        } catch (CancelledException e) {
            console.println("  [dim]Cancelled[/]");
        }
        console.println();

        // 3. ConfirmPrompt - Yes/No
        console.println("[bold]3. ConfirmPrompt[/] — Yes/No confirmation");
        try {
            boolean confirmed = ConfirmPrompt.ask("Do you like RichConsole?", console, true);
            console.println("  Your answer: [green]" + (confirmed ? "Yes" : "No") + "[/]");
        } catch (CancelledException e) {
            console.println("  [dim]Cancelled[/]");
        }
        console.println();

        // 4. InputPrompt - Text input
        console.println("[bold]4. InputPrompt[/] — Text input");
        try {
            String name = InputPrompt.ask("What is your name?", console);
            console.println("  Hello, [green]" + name + "[/]!");
        } catch (CancelledException e) {
            console.println("  [dim]Cancelled[/]");
        }
        console.println();

        // 5. InputPrompt with password
        console.println("[bold]5. InputPrompt[/] — Password input");
        try {
            String password = InputPrompt.askPassword("Enter your password:", console);
            console.println("  Password received ([dim]" + password.length() + " chars[/])");
        } catch (CancelledException e) {
            console.println("  [dim]Cancelled[/]");
        }

        console.println();
        console.println("[bold green]Demo complete![/]");
    }
}
