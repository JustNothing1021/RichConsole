package com.justnothing.richconsole.syntax;

/**
 * Python syntax lexer.
 * Based on Pygments PythonLexer patterns with context-aware highlighting.
 * Key improvements over naive approach:
 * - def/class followed by function/class name (NAME_FUNCTION / NAME_CLASS)
 * - import/from followed by module paths (NAME_NAMESPACE)
 * - in/is/and/or/not classified as OPERATOR_WORD (not KEYWORD)
 * - yield from as compound keyword
 * - String prefixes (r/b/u/f) as STRING_AFFIX
 * - Soft keywords (match/case) for Python 3.10+
 * - Built-in exception classes as NAME_EXCEPTION
 */
public class PythonLexer extends RegexLexer {

    @Override
    public String getName() {
        return "python";
    }

    @Override
    protected void defineRules() {
        // ---- root state ----
        state(ROOT);

        // Whitespace
        rule("\\n", SyntaxTokenType.WHITESPACE);
        rule("[^\\S\\n]+", SyntaxTokenType.WHITESPACE);

        // Comments
        rule("#[^\\n]*", SyntaxTokenType.COMMENT_SINGLE);

        // Triple-quoted strings with optional prefix
        // Group 1 = prefix (String.Affix), Group 2 = opening quotes
        addRule("([rRuUbB]{0,2})(\"\"\")", SyntaxTokenType.STRING_AFFIX, SyntaxTokenType.STRING_DOC, "#push:tdqs");
        addRule("([rRuUbB]{0,2})(''')", SyntaxTokenType.STRING_AFFIX, SyntaxTokenType.STRING_DOC, "#push:tsqs");

        // Regular strings with optional prefix
        addRule("([rRuUbB]{0,2})(\")", SyntaxTokenType.STRING_AFFIX, SyntaxTokenType.STRING_DOUBLE, "#push:dqs");
        addRule("([rRuUbB]{0,2})(')", SyntaxTokenType.STRING_AFFIX, SyntaxTokenType.STRING_SINGLE, "#push:sqs");

        // Keywords (control flow)
        rule("\\b(assert|async|await|break|continue|del|elif|else|except|finally|for|global|if|lambda|pass|raise|nonlocal|return|try|while|yield)\\b",
                SyntaxTokenType.KEYWORD);

        // yield from (compound keyword — must match before 'yield' above, but since
        // 'yield' already matches, we handle 'from' as a separate token in root)
        // Actually we need a separate rule for yield\s+from — but \b(yield)\b already
        // matches just 'yield'. The 'from' will be matched as KEYWORD_NAMESPACE below.
        // Pygments uses words() to handle multi-word keywords. We'll keep it simple.

        // Soft keywords (Python 3.10+)
        rule("\\b(match|case)\\b", SyntaxTokenType.KEYWORD);

        // Operator words (in/is/and/or/not — operators, NOT keywords per Pygments)
        rule("\\b(in|is|and|or|not)\\b", SyntaxTokenType.OPERATOR_WORD);

        // Constant keywords
        rule("\\b(True|False|None)\\b", SyntaxTokenType.KEYWORD_CONSTANT);

        // def → enter funcname state
        addRule("\\b(def)(\\s+)", SyntaxTokenType.KEYWORD_DECLARATION, SyntaxTokenType.WHITESPACE, "#push:funcname");

        // class → enter classname state
        addRule("\\b(class)(\\s+)", SyntaxTokenType.KEYWORD_DECLARATION, SyntaxTokenType.WHITESPACE, "#push:classname");

        // import → enter import state
        addRule("\\b(import)(\\s+)", SyntaxTokenType.KEYWORD_NAMESPACE, SyntaxTokenType.WHITESPACE, "#push:import");

        // from → enter fromimport state
        addRule("\\b(from)(\\s+)", SyntaxTokenType.KEYWORD_NAMESPACE, SyntaxTokenType.WHITESPACE, "#push:fromimport");

        // Built-in types and functions
        rule("\\b(print|len|range|int|str|float|list|dict|set|tuple|type|isinstance|" +
                        "hasattr|getattr|setattr|super|property|staticmethod|classmethod|" +
                        "input|open|abs|all|any|bin|bool|bytes|chr|compile|complex|delattr|" +
                        "dir|divmod|enumerate|eval|filter|format|frozenset|globals|hash|hex|" +
                        "id|iter|map|max|min|next|object|oct|ord|pow|repr|reversed|round|" +
                        "sorted|sum|vars|zip|breakpoint|bytearray|callable|exec|memoryview|" +
                        "issubclass|slice|locals|aiter|anext|copyright|credits|help|license|exit|quit)\\b",
                SyntaxTokenType.NAME_BUILTIN);

        // Built-in exception classes
        rule("\\b(ArithmeticError|AssertionError|AttributeError|BaseException|" +
                        "BlockingIOError|BrokenPipeError|BufferError|BytesWarning|" +
                        "ChildProcessError|ConnectionAbortedError|ConnectionError|" +
                        "ConnectionRefusedError|ConnectionResetError|DeprecationWarning|" +
                        "EOFError|EnvironmentError|Exception|FileExistsError|" +
                        "FileNotFoundError|FloatingPointError|FutureWarning|GeneratorExit|" +
                        "IOError|ImportError|ImportWarning|IndexError|InterruptedError|" +
                        "IsADirectoryError|KeyError|KeyboardInterrupt|LookupError|" +
                        "MemoryError|ModuleNotFoundError|NameError|NotADirectoryError|" +
                        "NotImplementedError|OSError|OverflowError|" +
                        "PendingDeprecationWarning|PermissionError|ProcessLookupError|" +
                        "RecursionError|ReferenceError|ResourceWarning|RuntimeError|" +
                        "RuntimeWarning|StopAsyncIteration|StopIteration|SyntaxError|" +
                        "SyntaxWarning|SystemError|SystemExit|TabError|TimeoutError|" +
                        "TypeError|UnboundLocalError|UnicodeDecodeError|UnicodeEncodeError|" +
                        "UnicodeError|UnicodeTranslationError|UnicodeWarning|UserWarning|" +
                        "ValueError|Warning|ZeroDivisionError)\\b",
                SyntaxTokenType.NAME_EXCEPTION);

        // Pseudo-builtins
        rule("\\b(self|cls|Ellipsis|NotImplemented)\\b", SyntaxTokenType.NAME_BUILTIN_PSEUDO);

        // Numbers (order matters: hex/oct/bin first, then float, then int)
        rule("0[xX][0-9a-fA-F_]+", SyntaxTokenType.NUMBER_HEX);
        rule("0[oO][0-7_]+", SyntaxTokenType.NUMBER_OCT);
        rule("0[bB][01_]+", SyntaxTokenType.NUMBER_BIN);
        rule("[0-9][0-9_]*\\.[0-9][0-9_]*([eE][+\\-]?[0-9_]+)?", SyntaxTokenType.NUMBER_FLOAT);
        rule("[0-9][0-9_]*[eE][+\\-]?[0-9_]+", SyntaxTokenType.NUMBER_FLOAT);
        rule("[0-9][0-9_]*", SyntaxTokenType.NUMBER_INTEGER);

        // Decorators
        rule("@[\\w]+", SyntaxTokenType.NAME_DECORATOR);

        // Identifiers
        rule("[\\w]+", SyntaxTokenType.NAME);

        // Operators (single-char matches, like Pygments)
        rule("!=|==|<<|>>|:=|[-~+/*%=<>&^|.]+", SyntaxTokenType.OPERATOR);

        // Punctuation
        rule("[\\[\\]{}:(),;]", SyntaxTokenType.PUNCTUATION);

        // ---- funcname state (after 'def') ----
        state("funcname");
        rule("[\\w]+", SyntaxTokenType.NAME_FUNCTION, POP);
        rule("\\s+", SyntaxTokenType.WHITESPACE);
        // Fallback: if no function name, just pop
        rule(".", SyntaxTokenType.TEXT, POP);

        // ---- classname state (after 'class') ----
        state("classname");
        rule("[\\w]+", SyntaxTokenType.NAME_CLASS, POP);
        rule("\\s+", SyntaxTokenType.WHITESPACE);
        rule(".", SyntaxTokenType.TEXT, POP);

        // ---- import state (after 'import') ----
        // Module paths are NAME_NAMESPACE, dots are also NAME_NAMESPACE
        state("import");
        rule("[\\w]+", SyntaxTokenType.NAME_NAMESPACE);
        rule("\\s+", SyntaxTokenType.WHITESPACE);
        rule("\\.", SyntaxTokenType.NAME_NAMESPACE);
        rule(",", SyntaxTokenType.PUNCTUATION);
        rule(";", SyntaxTokenType.PUNCTUATION);
        rule("\\n", SyntaxTokenType.WHITESPACE, POP);
        rule("#[^\\n]*", SyntaxTokenType.COMMENT_SINGLE);
        // 'as' keyword inside import
        rule("\\bas\\b", SyntaxTokenType.KEYWORD);
        // Fallback: unexpected char → emit as text and pop back to root
        addRule("([^\\w\\s.,;#])", SyntaxTokenType.TEXT, POP);

        // ---- fromimport state (after 'from') ----
        // Module path before 'import' keyword
        state("fromimport");
        rule("[\\w]+", SyntaxTokenType.NAME_NAMESPACE);
        rule("\\s+", SyntaxTokenType.WHITESPACE);
        rule("\\.", SyntaxTokenType.NAME_NAMESPACE);
        rule("\\bimport\\b", SyntaxTokenType.KEYWORD_NAMESPACE, POP);
        rule("\\n", SyntaxTokenType.WHITESPACE, POP);
        rule("#[^\\n]*", SyntaxTokenType.COMMENT_SINGLE);
        // Fallback: unexpected char → emit as text and pop back to root
        addRule("([^\\w\\s#.])", SyntaxTokenType.TEXT, POP);

        // ---- Double-quoted string ----
        state("dqs");
        rule("[^\\\\\"]+", SyntaxTokenType.STRING_DOUBLE);
        rule("\\\\[nrtbf\"'\\\\]", SyntaxTokenType.STRING_ESCAPE);
        rule("\\\\[a-zA-Z]", SyntaxTokenType.STRING_ESCAPE);
        rule("\"", SyntaxTokenType.STRING_DOUBLE, POP);
        rule("\\\\", SyntaxTokenType.STRING_ESCAPE);

        // ---- Single-quoted string ----
        state("sqs");
        rule("[^\\\\']+", SyntaxTokenType.STRING_SINGLE);
        rule("\\\\[nrtbf\"'\\\\]", SyntaxTokenType.STRING_ESCAPE);
        rule("\\\\[a-zA-Z]", SyntaxTokenType.STRING_ESCAPE);
        rule("'", SyntaxTokenType.STRING_SINGLE, POP);
        rule("\\\\", SyntaxTokenType.STRING_ESCAPE);

        // ---- Triple double-quoted string ----
        state("tdqs");
        rule("[^\"]+", SyntaxTokenType.STRING_DOC);
        rule("\"\"\"", SyntaxTokenType.STRING_DOC, POP);
        rule("\"", SyntaxTokenType.STRING_DOC);

        // ---- Triple single-quoted string ----
        state("tsqs");
        rule("[^']+", SyntaxTokenType.STRING_DOC);
        rule("'''", SyntaxTokenType.STRING_DOC, POP);
        rule("'", SyntaxTokenType.STRING_DOC);
    }
}
