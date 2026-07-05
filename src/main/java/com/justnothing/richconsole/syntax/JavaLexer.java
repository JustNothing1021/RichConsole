package com.justnothing.richconsole.syntax;

/**
 * Java syntax lexer.
 * Inspired by Pygments JavaLexer and VSCode-style highlighting.
 *
 * <p>Keyword categories (matching VSCode/IntelliJ conventions):</p>
 * <ul>
 *   <li>{@code KEYWORD} — control flow: if, else, for, while, return, etc.</li>
 *   <li>{@code KEYWORD_DECLARATION} — modifiers: public, private, static, final, etc.</li>
 *   <li>{@code KEYWORD_TYPE} — primitives: int, boolean, void, etc.</li>
 *   <li>{@code KEYWORD_CONSTANT} — literals: true, false, null</li>
 *   <li>{@code NAME_FUNCTION} — identifiers followed by {@code (}</li>
 *   <li>{@code NAME_CLASS} — identifiers starting with uppercase</li>
 *   <li>{@code NAME} — other identifiers</li>
 * </ul>
 */
public class JavaLexer extends RegexLexer {

    @Override
    public String getName() {
        return "java";
    }

    @Override
    protected void defineRules() {
        // ---- root state ----
        state(ROOT);

        // Whitespace
        rule("[^\\S\\n]+", SyntaxTokenType.WHITESPACE);
        rule("\\n", SyntaxTokenType.WHITESPACE);

        // Comments (must come before keywords/operators)
        rule("//[^\\n]*", SyntaxTokenType.COMMENT_SINGLE);
        addRule("/\\*", SyntaxTokenType.COMMENT_MULTILINE, "#push:comment");

        // Text blocks (""" ...)
        addRule("\"\"\"", SyntaxTokenType.STRING, "#push:multiline_string");

        // Regular strings
        addRule("\"", SyntaxTokenType.STRING, "#push:string");

        // Char literals
        rule("'(?:\\\\.|[^\\\\'])'", SyntaxTokenType.STRING_CHAR);

        // Annotations
        rule("@[\\w.]+", SyntaxTokenType.NAME_DECORATOR);

        // Control flow keywords
        rule("\\b(assert|break|case|catch|continue|default|do|else|finally|for|goto|if|instanceof|new|return|switch|throw|try|while|yield)\\b",
                SyntaxTokenType.KEYWORD);

        // Modifier / declaration keywords
        rule("\\b(abstract|class|const|enum|exports|extends|final|implements|import|interface|module|native|non-sealed|open|opens|package|permits|private|protected|provides|public|record|requires|sealed|static|strictfp|super|synchronized|this|throws|to|transient|transitive|uses|volatile|with)\\b",
                SyntaxTokenType.KEYWORD_DECLARATION);

        // Primitive types
        rule("\\b(boolean|byte|char|double|float|int|long|short|void|var)\\b",
                SyntaxTokenType.KEYWORD_TYPE);

        // Constants
        rule("\\b(true|false|null)\\b", SyntaxTokenType.KEYWORD_CONSTANT);

        // Numbers (must come before identifiers)
        rule("0[xX][0-9a-fA-F][0-9a-fA-F_]*[lL]?", SyntaxTokenType.NUMBER_HEX);
        rule("0[bB][01][01_]*[lL]?", SyntaxTokenType.NUMBER_BIN);
        rule("0[0-7_]+[lL]?", SyntaxTokenType.NUMBER_OCT);
        rule("[0-9][0-9_]*\\.[0-9][0-9_]*([eE][+\\-]?[0-9][0-9_]*)?[fFdD]?",
                SyntaxTokenType.NUMBER_FLOAT);
        rule("\\.[0-9][0-9_]*([eE][+\\-]?[0-9][0-9_]*)?[fFdD]?",
                SyntaxTokenType.NUMBER_FLOAT);
        rule("[0-9][0-9_]*[eE][+\\-]?[0-9][0-9_]*[fFdD]?",
                SyntaxTokenType.NUMBER_FLOAT);
        rule("[0-9][0-9_]*[lL]?", SyntaxTokenType.NUMBER_INTEGER);

        // Function call: identifier followed by (
        // Uses capturing groups: group1=identifier, group2=(
        addRule("\\b([a-zA-Z_$][\\w$]*)\\s*(\\()", SyntaxTokenType.NAME_FUNCTION, SyntaxTokenType.PUNCTUATION);

        // All-uppercase constants (e.g., MAX, MIN_VALUE, serialVersionUID)
        rule("\\b[A-Z][A-Z0-9_$]*\\b", SyntaxTokenType.NAME_CONSTANT);

        // Class-like identifiers (uppercase start but not all uppercase)
        rule("\\b[A-Z][a-zA-Z0-9_$]*\\b", SyntaxTokenType.NAME_CLASS);

        // Other identifiers
        rule("[\\w$]+", SyntaxTokenType.NAME);

        // Operators
        rule("[~^*!%&\\[\\]<>|+=/?\\-:]+", SyntaxTokenType.OPERATOR);

        // Punctuation
        rule("[{}();,]", SyntaxTokenType.PUNCTUATION);

        // ---- string state ----
        state("string");
        rule("[^\\\\\"]+", SyntaxTokenType.STRING);
        rule("\\\\[nrtbf'\"\\\\]", SyntaxTokenType.STRING_ESCAPE);
        rule("\\\\u[0-9a-fA-F]{4}", SyntaxTokenType.STRING_ESCAPE);
        rule("\"", SyntaxTokenType.STRING, POP);
        rule("\\\\", SyntaxTokenType.STRING_ESCAPE);

        // ---- multiline_string state (Java text blocks) ----
        state("multiline_string");
        rule("\"\"\"", SyntaxTokenType.STRING, POP);
        rule("[^\"]+", SyntaxTokenType.STRING);
        rule("\"", SyntaxTokenType.STRING);

        // ---- comment state (block comments) ----
        state("comment");
        rule("[^*]+", SyntaxTokenType.COMMENT_MULTILINE);
        rule("\\*\\/", SyntaxTokenType.COMMENT_MULTILINE, POP);
        rule("\\*", SyntaxTokenType.COMMENT_MULTILINE);
    }
}
