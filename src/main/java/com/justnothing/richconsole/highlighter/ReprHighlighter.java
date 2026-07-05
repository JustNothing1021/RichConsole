package com.justnothing.richconsole.highlighter;

import java.util.List;

/**
 * Highlights the text typically produced from __repr__ methods.
 * Ported from rich/highlighter.py ReprHighlighter.
 */
public class ReprHighlighter extends RegexHighlighter {

    private static String combineRegex(String... regexes) {
        return String.join("|", regexes);
    }

    @Override
    public String getBaseStyle() {
        return "repr.";
    }

    @Override
    public List<String> getHighlights() {
        return java.util.Arrays.asList(
            r("(?<tagStart><)(?<tagName>[-\\w.:|]*)(?<tagContents>[\\w\\W]*)(?<tagEnd>>)"),
            r("(?<attribName>[\\w_]{1,50})=(?<attribValue>\"?[\\w_]+\"?)?"),
            r("(?<brace>[\\[\\]{}()])"),
            combineRegex(
                r("(?<ipv4>[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})"),
                r("(?<uuid>[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"),
                r("(?<call>[\\w.]*?)\\("),
                r("\\b(?<boolTrue>true|True)\\b|\\b(?<boolFalse>false|False)\\b|\\b(?<none>null|None)\\b"),
                r("(?<ellipsis>\\.\\.\\.)"),
                r("(?<number>(?<!\\w)\\-?[0-9]+\\.?[0-9]*(e[\\-+]?\\d+?)?\\b|0x[0-9a-fA-F]*)"),
                r("(?<path>\\B(/[-\\w._+]+)*\\/)(?<filename>[-\\w._+]*)?"),
                r("(?<![\\\\\\w])(?<str>b?'''.*?(?<!\\\\)'''|b?'.*?(?<!\\\\)'|b?\"\"\".*?(?<!\\\\)\"\"\"|b?\".*?(?<!\\\\)\")"),
                r("(?<url>(file|https?|ws[s]?)://[-0-9a-zA-Z$_+!`(),.?/;:&=%#~@]*)")
            )
        );
    }

    private static String r(String s) {
        return s;
    }
}
