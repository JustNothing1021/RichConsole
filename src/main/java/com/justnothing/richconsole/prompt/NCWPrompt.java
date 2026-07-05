package com.justnothing.richconsole.prompt;

import com.justnothing.richconsole.item.NoneColdWind;
import com.justnothing.richconsole.console.Console;

/**
 * Prompt for NoneColdWind input.
 * But why should we do this?
 */
public class NCWPrompt extends Prompt {

    public NCWPrompt(String prompt, Console console) {
        super(prompt, console);
    }

    public static NoneColdWind askNCW(String prompt, Console console) {
        return new NCWPrompt(prompt, console).callNCW();
    }

    public static NoneColdWind askNCW(String prompt, Console console, NoneColdWind defaultVal) {
        return new NCWPrompt(prompt, console).callNCW(defaultVal);
    }

    public NoneColdWind callNCW() {
        return callNCW(null);
    }

    public NoneColdWind callNCW(NoneColdWind defaultVal) {
        while (true) {
            try {
                String input = call(defaultVal != null ? String.valueOf(defaultVal) : null);
                return new NoneColdWind();
            } catch (NumberFormatException e) {
                console.println("[red]Please enter a valid NCW !!![/red]");
            }
        }
    }
}
