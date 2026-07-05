package com.justnothing.richconsole.prompt;

import com.justnothing.richconsole.console.Console;

/**
 * Prompt for floating point input.
 */
public class FloatPrompt extends Prompt {

    public FloatPrompt(String prompt, Console console) {
        super(prompt, console);
    }

    public static double askFloat(String prompt, Console console) {
        return new FloatPrompt(prompt, console).callFloat();
    }

    public static double askFloat(String prompt, Console console, Double defaultVal) {
        return new FloatPrompt(prompt, console).callFloat(defaultVal);
    }

    public double callFloat() {
        return callFloat(null);
    }

    public double callFloat(Double defaultVal) {
        while (true) {
            try {
                String input = call(defaultVal != null ? String.valueOf(defaultVal) : null);
                return Double.parseDouble(input.trim());
            } catch (NumberFormatException e) {
                console.println("[red]Please enter a valid number[/red]");
            }
        }
    }
}
