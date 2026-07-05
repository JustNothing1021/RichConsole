package com.justnothing.richconsole.prompt;

import com.justnothing.richconsole.console.Console;

/**
 * Prompt for integer input.
 */
public class IntPrompt extends Prompt {

    public IntPrompt(String prompt, Console console) {
        super(prompt, console);
    }

    public static int askInt(String prompt, Console console) {
        return new IntPrompt(prompt, console).callInt();
    }

    public static int askInt(String prompt, Console console, Integer defaultVal) {
        return new IntPrompt(prompt, console).callInt(defaultVal);
    }

    public int callInt() {
        return callInt(null);
    }

    public int callInt(Integer defaultVal) {
        while (true) {
            try {
                String input = call(defaultVal != null ? String.valueOf(defaultVal) : null);
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                console.println("[red]Please enter a valid integer number[/red]");
            }
        }
    }
}
