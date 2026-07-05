package com.justnothing.richconsole.syntax;

/**
 * JSON syntax lexer.
 * Simplified version for terminal rendering.
 */
public class JsonLexer extends RegexLexer {

    @Override
    public String getName() {
        return "json";
    }

    @Override
    protected void defineRules() {
        // ---- root state ----
        state(ROOT);

        // Whitespace
        rule("[\\s\\n]+", SyntaxTokenType.WHITESPACE);

        // Strings (JSON keys and values)
        addRule("\"", SyntaxTokenType.STRING_DOUBLE, "#push:string");

        // Constants
        rule("\\b(true|false|null)\\b", SyntaxTokenType.KEYWORD_CONSTANT);

        // Numbers
        rule("-?[0-9]+\\.[0-9]+([eE][+\\-]?[0-9]+)?", SyntaxTokenType.NUMBER_FLOAT);
        rule("-?[0-9]+[eE][+\\-]?[0-9]+", SyntaxTokenType.NUMBER_FLOAT);
        rule("-?[0-9]+", SyntaxTokenType.NUMBER_INTEGER);

        // Punctuation
        rule("[\\[\\]{}:,]", SyntaxTokenType.PUNCTUATION);

        // ---- string state ----
        state("string");
        rule("[^\\\\\"]+", SyntaxTokenType.STRING_DOUBLE);
        rule("\\\\[\"\\\\/bfnrt]", SyntaxTokenType.STRING_ESCAPE);
        rule("\\\\u[0-9a-fA-F]{4}", SyntaxTokenType.STRING_ESCAPE);
        rule("\"", SyntaxTokenType.STRING_DOUBLE, POP);
        rule("\\\\", SyntaxTokenType.STRING_ESCAPE);
    }
}
