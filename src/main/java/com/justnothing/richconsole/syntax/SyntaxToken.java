package com.justnothing.richconsole.syntax;

/**
 * A token produced by a Lexer, consisting of a type and text content.
 */
public class SyntaxToken {
    public final String type;
    public final String text;

    public SyntaxToken(String type, String text) {
        this.type = type;
        this.text = text;
    }

    @Override
    public String toString() {
        return "SyntaxToken(" + type + ", " + repr(text) + ")";
    }

    private static String repr(String s) {
        if (s.length() > 40) {
            return "\"" + s.substring(0, 37) + "...\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
