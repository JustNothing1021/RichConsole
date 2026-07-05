package com.justnothing.richconsole.syntax;

/**
 * Token types for syntax highlighting.
 * Uses dot-separated strings for hierarchical matching (e.g., "keyword.declaration" is a subtype of "keyword").
 * Inspired by Pygments' Token hierarchy.
 */
public final class SyntaxTokenType {
    // Prevent instantiation
    private SyntaxTokenType() {}

    // ---- Root ----
    public static final String TOKEN = "token";

    // ---- Text ----
    public static final String TEXT = "text";
    public static final String WHITESPACE = "text.whitespace";

    // ---- Comment ----
    public static final String COMMENT = "comment";
    public static final String COMMENT_SINGLE = "comment.single";
    public static final String COMMENT_MULTILINE = "comment.multiline";
    public static final String COMMENT_SPECIAL = "comment.special";

    // ---- Keyword ----
    public static final String KEYWORD = "keyword";
    public static final String KEYWORD_CONSTANT = "keyword.constant";
    public static final String KEYWORD_DECLARATION = "keyword.declaration";
    public static final String KEYWORD_NAMESPACE = "keyword.namespace";
    public static final String KEYWORD_TYPE = "keyword.type";
    public static final String KEYWORD_RESERVED = "keyword.reserved";

    // ---- Name (identifiers) ----
    public static final String NAME = "name";
    public static final String NAME_FUNCTION = "name.function";
    public static final String NAME_CLASS = "name.class";
    public static final String NAME_NAMESPACE = "name.namespace";
    public static final String NAME_BUILTIN = "name.builtin";
    public static final String NAME_BUILTIN_PSEUDO = "name.builtin.pseudo";
    public static final String NAME_EXCEPTION = "name.exception";
    public static final String NAME_DECORATOR = "name.decorator";
    public static final String NAME_VARIABLE = "name.variable";
    public static final String NAME_LABEL = "name.label";
    public static final String NAME_ATTRIBUTE = "name.attribute";
    public static final String NAME_TAG = "name.tag";
    public static final String NAME_CONSTANT = "name.constant";

    // ---- String ----
    public static final String STRING = "string";
    public static final String STRING_DOUBLE = "string.double";
    public static final String STRING_SINGLE = "string.single";
    public static final String STRING_DOC = "string.doc";
    public static final String STRING_ESCAPE = "string.escape";
    public static final String STRING_INTERPOL = "string.interpol";
    public static final String STRING_AFFIX = "string.affix";
    public static final String STRING_CHAR = "string.char";

    // ---- Number ----
    public static final String NUMBER = "number";
    public static final String NUMBER_INTEGER = "number.integer";
    public static final String NUMBER_FLOAT = "number.float";
    public static final String NUMBER_HEX = "number.hex";
    public static final String NUMBER_OCT = "number.oct";
    public static final String NUMBER_BIN = "number.bin";

    // ---- Operator ----
    public static final String OPERATOR = "operator";
    public static final String OPERATOR_WORD = "operator.word";

    // ---- Punctuation ----
    public static final String PUNCTUATION = "punctuation";

    // ---- Error ----
    public static final String ERROR = "error";

    /**
     * Check if a token type matches a parent type using prefix matching.
     * For example, "keyword.declaration" matches "keyword".
     */
    public static boolean isSubtype(String type, String parent) {
        if (type == null || parent == null) return false;
        if (type.equals(parent)) return true;
        return type.startsWith(parent + ".");
    }
}
