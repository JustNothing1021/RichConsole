package com.justnothing.richconsole.syntax;

import java.util.List;

/**
 * Interface for syntax highlighting lexers.
 * Produces a list of tokens from source code.
 */
public interface Lexer {
    /**
     * Tokenize the given source code into a list of tokens.
     */
    List<SyntaxToken> tokenize(String code);

    /**
     * Get the name of this lexer (e.g., "java", "python", "json").
     */
    String getName();
}
