package com.justnothing.richconsole.syntax;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for looking up lexers by name or filename.
 */
public class LexerRegistry {

    private static final Map<String, Lexer> BY_NAME = new HashMap<>();
    private static final Map<String, Lexer> BY_EXTENSION = new HashMap<>();

    static {
        JavaLexer javaLexer = new JavaLexer();
        PythonLexer pythonLexer = new PythonLexer();
        JsonLexer jsonLexer = new JsonLexer();

        register(javaLexer, "java", new String[]{".java"});
        register(pythonLexer, "python", new String[]{".py", ".pyw", ".pyi"});
        register(jsonLexer, "json", new String[]{".json", ".jsonl"});

        // Aliases
        BY_NAME.put("py", pythonLexer);
        BY_NAME.put("py3", pythonLexer);
    }

    private static void register(Lexer lexer, String name, String[] extensions) {
        BY_NAME.put(name, lexer);
        for (String ext : extensions) {
            BY_EXTENSION.put(ext, lexer);
        }
    }

    /**
     * Get a lexer by language name.
     */
    public static Lexer getByName(String name) {
        if (name == null) return null;
        return BY_NAME.get(name.toLowerCase());
    }

    /**
     * Get a lexer by file extension.
     */
    public static Lexer getByExtension(String extension) {
        if (extension == null) return null;
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return BY_EXTENSION.get(extension.toLowerCase());
    }

    /**
     * Get a lexer by language name, falling back to file extension.
     */
    public static Lexer get(String nameOrExtension) {
        Lexer lexer = getByName(nameOrExtension);
        if (lexer == null) {
            lexer = getByExtension(nameOrExtension);
        }
        return lexer;
    }
}
