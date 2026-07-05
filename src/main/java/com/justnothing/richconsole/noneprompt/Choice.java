package com.justnothing.richconsole.noneprompt;

/**
 * A choice item for list-like prompts.
 * Ported from noneprompt/Choice.
 *
 * @param <T> the type of data associated with this choice
 */
public record Choice<T>(String name, T data) {

    public Choice(String name) {
        this(name, null);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Create a Choice from just a name string.
     */
    public static Choice<String> of(String name) {
        return new Choice<>(name, name);
    }
}
