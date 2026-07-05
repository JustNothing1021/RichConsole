package com.justnothing.richconsole.prompt;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.text.Text;

/**
 * Interactive prompt for user input.
 * Ported from rich/prompt.py.
 *
 * Usage:
 * <pre>
 *   String name = Prompt.ask("What is your name?", console);
 *   int age = IntPrompt.ask("How old are you?", console);
 *   boolean proceed = Confirm.ask("Continue?", console, true);
 * </pre>
 */
public class Prompt {

    protected final Console console;
    protected final String prompt;
    protected final java.util.List<String> choices;
    protected final boolean caseSensitive;
    protected final boolean showDefault;
    protected final boolean showChoices;
    protected final boolean password;

    public Prompt(String prompt, Console console) {
        this(prompt, console, null, true, true, true, false);
    }

    public Prompt(String prompt, Console console, java.util.List<String> choices,
                  boolean caseSensitive, boolean showDefault, boolean showChoices, boolean password) {
        this.prompt = prompt;
        this.console = console;
        this.choices = choices;
        this.caseSensitive = caseSensitive;
        this.showDefault = showDefault;
        this.showChoices = showChoices;
        this.password = password;
    }

    /**
     * Convenience method to prompt and return input in one call.
     */
    public static String ask(String prompt, Console console) {
        return new Prompt(prompt, console).call();
    }

    /**
     * Convenience method with a default value.
     */
    public static String ask(String prompt, Console console, String defaultVal) {
        return new Prompt(prompt, console).call(defaultVal);
    }

    /**
     * Convenience method with choices.
     */
    public static String ask(String prompt, Console console, java.util.List<String> choices, String defaultVal) {
        return new Prompt(prompt, console, choices, true, true, true, false).call(defaultVal);
    }

    /**
     * Run the prompt loop until valid input is received.
     */
    public String call() {
        return call(null);
    }

    /**
     * Run the prompt loop with a default value.
     */
    public String call(String defaultVal) {
        while (true) {
            try {
                Text promptText = makePrompt(defaultVal);
                String input = getInput(promptText);
                if (input.isEmpty() && defaultVal != null) {
                    return defaultVal;
                }
                return processResponse(input);
            } catch (InvalidResponse e) {
                console.println("[red]" + e.getMessage() + "[/red]");
            }
        }
    }

    /**
     * Build the full prompt Text (prompt + choices + default + suffix).
     */
    protected Text makePrompt(String defaultVal) {
        Text text = Text.fromMarkup(prompt);
        if (showChoices && choices != null && !choices.isEmpty()) {
            text.append(" [");
            text.append(String.join("/", choices), "prompt.choices");
            text.append("]");
        }
        if (showDefault && defaultVal != null) {
            text.append(" (");
            text.append(renderDefault(defaultVal));
            text.append(")", "prompt.default");
        }
        text.append(": ");
        return text;
    }

    /**
     * Render the default value as text.
     */
    protected String renderDefault(String defaultVal) {
        return defaultVal;
    }

    /**
     * Get raw input from user via JLine.
     */
    protected String getInput(Text promptText) {
        return console.input(promptText, password);
    }

    /**
     * Process and validate the user's response.
     */
    protected String processResponse(String value) throws InvalidResponse {
        if (choices != null && !choices.isEmpty()) {
            String checkValue = caseSensitive ? value : value.toLowerCase();
            for (String choice : choices) {
                String checkChoice = caseSensitive ? choice : choice.toLowerCase();
                if (checkValue.equals(checkChoice)) {
                    return value;
                }
            }
            throw new InvalidResponse("Please select from " + choices);
        }
        return value;
    }

    // =========================================================================
    // InvalidResponse exception
    // =========================================================================

    public static class InvalidResponse extends Exception {
        public InvalidResponse(String message) {
            super(message);
        }
    }
}
