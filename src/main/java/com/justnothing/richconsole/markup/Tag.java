package com.justnothing.richconsole.markup;

/**
 * Represents a markup tag with a name and optional parameters.
 * Ported from rich/markup.py Tag named tuple.
 */
public record Tag(String name, String parameters) {

    /**
     * Return the markup string representation of this tag.
     */
    public String markup() {
        if (parameters != null && !parameters.isEmpty()) {
            return "[" + name + "=" + parameters + "]";
        }
        return "[" + name + "]";
    }

    @Override
    public String toString() {
        if (parameters != null && !parameters.isEmpty()) {
            return "Tag(" + name + ", " + parameters + ")";
        }
        return "Tag(" + name + ")";
    }
}
