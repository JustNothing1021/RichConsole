package com.justnothing.richconsole.prompt;

import java.util.List;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.text.Text;

/**
 * Prompt for yes/no confirmation.
 * Ported from rich/prompt.py Confirm.
 *
 * Usage:
 * <pre>
 *   if (Confirm.ask("Continue?", console, true)) {
 *       // user confirmed
 *   }
 * </pre>
 */
public class Confirm extends Prompt {

    private static final List<String> YES_NO = java.util.Arrays.asList("y", "n");

    public Confirm(String prompt, Console console) {
        super(prompt, console, YES_NO, false, true, true, false);
    }

    /**
     * Convenience method: ask a yes/no question.
     *
     * @param prompt    the question to ask
     * @param console   the console to use
     * @param defaultVal the default value (true=yes, false=no)
     * @return true if user confirmed, false otherwise
     */
    public static boolean askConfirm(String prompt, Console console, boolean defaultVal) {
        return new Confirm(prompt, console).callBool(defaultVal);
    }

    /**
     * Convenience method without default value.
     */
    public static boolean askConfirm(String prompt, Console console) {
        return new Confirm(prompt, console).callBool(null);
    }

    public boolean callBool(Boolean defaultVal) {
        while (true) {
            try {
                Text promptText = makePrompt(defaultVal);
                String input = getInput(promptText);
                if (input.isEmpty() && defaultVal != null) {
                    return defaultVal;
                }
                return processBoolResponse(input);
            } catch (InvalidResponse e) {
                console.println("[red]" + e.getMessage() + "[/red]");
            }
        }
    }

    @Override
    protected String renderDefault(String defaultVal) {
        // Show (y) for true, (n) for false
        if ("true".equalsIgnoreCase(defaultVal)) return "y";
        if ("false".equalsIgnoreCase(defaultVal)) return "n";
        return defaultVal;
    }

    protected Text makePrompt(Boolean defaultVal) {
        return makePrompt(defaultVal != null ? String.valueOf(defaultVal) : null);
    }

    private boolean processBoolResponse(String value) throws InvalidResponse {
        String trimmed = value.trim().toLowerCase();
        if ("y".equals(trimmed)) return true;
        if ("n".equals(trimmed)) return false;
        throw new InvalidResponse("Please enter Y or N");
    }
}
