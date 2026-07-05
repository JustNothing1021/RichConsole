package com.justnothing.richconsole.highlighter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.justnothing.richconsole.text.Text;

/**
 * Highlights JSON text.
 * Ported from rich/highlighter.py JSONHighlighter.
 *
 * In addition to standard regex highlighting, detects JSON keys
 * (strings followed by a colon) and applies "json.key" style.
 */
public class JSONHighlighter extends RegexHighlighter {

    private static final String JSON_STR = "(?<![\\\\\\w])(?<str>b?\\\".*?(?<!\\\\)\\\")";
    private static final java.util.Set<Character> JSON_WHITESPACE = java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList(' ', '\n', '\r', '\t')));

    @Override
    public String getBaseStyle() {
        return "json.";
    }

    @Override
    public List<String> getHighlights() {
        return java.util.Arrays.asList(
            combineRegex(
                r("(?<brace>[\\{\\[\\(\\)\\]\\}])"),
                r("\\b(?<boolTrue>true)\\b|\\b(?<boolFalse>false)\\b|\\b(?<null>null)\\b"),
                r("(?<number>(?<!\\w)\\-?[0-9]+\\.?[0-9]*(e[\\-+]?\\d+?)?\\b|0x[0-9a-fA-F]*)"),
                JSON_STR
            )
        );
    }

    @Override
    public void highlight(Text text) {
        super.highlight(text);

        // Additional work: highlight JSON keys
        // Find all string matches, then check if followed by ':'
        String plain = text.getPlain();
        Pattern p = Pattern.compile(JSON_STR);
        Matcher m = p.matcher(plain);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            int cursor = end;
            while (cursor < plain.length()) {
                char ch = plain.charAt(cursor);
                cursor++;
                if (ch == ':') {
                    text.stylize("json.key", start, end);
                    break;
                } else if (JSON_WHITESPACE.contains(ch)) {
                    // continue
                } else {
                    break;
                }
            }
        }
    }

    private static String combineRegex(String... regexes) {
        return String.join("|", regexes);
    }

    private static String r(String s) {
        return s;
    }
}
