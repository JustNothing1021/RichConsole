package com.justnothing.richconsole.markup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.justnothing.richconsole.errors.MarkupError;

/**
 * Markup parsing utilities for Rich-style markup strings.
 * Ported from rich/markup.py.
 *
 * <p>Markup syntax uses square brackets for styling, e.g.:
 * [bold]Hello[/bold] [italic]World[/italic]
 * [bold red]Alert![/bold red]
 * [link=https://example.com]Click[/link]</p>
 *
 * <p>Since the Text class doesn't exist yet, parse() returns a simplified
 * structure: a list of StyledSpan objects, each containing text and style string.</p>
 */
public final class Markup {

    private Markup() {}

    /**
     * A simplified span of text with a style, used until the Text class is available.
     */
    public record StyledSpan(String text, String style) {}

    /**
     * Regex pattern for matching markup tags.
     */
    private static final String TAG_PATTERN = "\\[(/?)([^\\]=]+)(?:=([^\\]]*))?]";

    /**
     * Escape square brackets in a string so they are not interpreted as markup.
     *
     * @param markup the markup string to escape
     * @return the escaped string with brackets replaced by their escaped forms
     */
    public static String escape(String markup) {
        if (markup == null) {
            return "";
        }
        return markup.replace("\\[", "\\\\[").replace("\\]", "\\\\]");
    }

    /**
     * Parse a markup string into a list of styled spans.
     *
     * <p>This is a simplified implementation that returns StyledSpan objects
     * instead of a Text instance. It handles basic nesting of tags.</p>
     *
     * @param markup the markup string to parse
     * @return a list of StyledSpan objects
     * @throws MarkupError if the markup is malformed
     */
    public static List<StyledSpan> parse(String markup) {
        if (markup == null || markup.isEmpty()) {
            return Collections.emptyList();
        }

        List<StyledSpan> spans = new ArrayList<>();
        List<String> styleStack = new ArrayList<>();

        int pos = 0;
        int length = markup.length();

        while (pos < length) {
            // Look for the next tag
            int bracketPos = markup.indexOf('[', pos);

            if (bracketPos < 0) {
                // No more tags; rest is plain text
                String text = markup.substring(pos);
                if (!text.isEmpty()) {
                    spans.add(new StyledSpan(text, currentStyle(styleStack)));
                }
                break;
            }

            // Add text before the bracket
            if (bracketPos > pos) {
                String text = markup.substring(pos, bracketPos);
                spans.add(new StyledSpan(text, currentStyle(styleStack)));
            }

            // Check for escaped bracket
            if (bracketPos > 0 && markup.charAt(bracketPos - 1) == '\\') {
                // Already handled as part of text above
                pos = bracketPos + 1;
                continue;
            }

            // Find the closing bracket
            int closePos = markup.indexOf(']', bracketPos);
            if (closePos < 0) {
                throw new MarkupError("Unclosed markup tag at position " + bracketPos);
            }

            // Parse the tag content
            String tagContent = markup.substring(bracketPos + 1, closePos);
            boolean isClosing = false;

            if (tagContent.startsWith("/")) {
                isClosing = true;
                tagContent = tagContent.substring(1);
            }

            if (isClosing) {
                // Closing tag
                if (styleStack.isEmpty()) {
                    throw new MarkupError("Unexpected closing tag: [/" + tagContent + "]");
                }
                String expected = styleStack.remove(styleStack.size() - 1);
                // Simple matching: just pop the last style
                // Rich allows un-nested closing as long as it matches somewhere
                if (!tagContent.equals(expected) && !styleContains(styleStack, tagContent)) {
                    // Try to find it in the stack
                    int idx = styleStack.lastIndexOf(tagContent);
                    if (idx >= 0) {
                        // Pop back to that point
                        while (styleStack.size() > idx) {
                            styleStack.remove(styleStack.size() - 1);
                        }
                    }
                }
            } else {
                // Opening tag
                if (!tagContent.isEmpty()) {
                    styleStack.add(tagContent);
                }
            }

            pos = closePos + 1;
        }

        return spans;
    }

    /**
     * Render markup to a list of styled spans.
     * This is a simplified version until the Text class is available.
     *
     * @param markup the markup string
     * @param style  base style to apply (may be null)
     * @param emoji  whether to process emoji (currently unused)
     * @return a list of StyledSpan objects
     */
    // TODO: implement emoji replacement (:code: → Unicode emoji) when emoji=true, matching Python's _emoji_replace()
    public static List<StyledSpan> render(String markup, String style, boolean emoji) {
        List<StyledSpan> spans = parse(markup);
        if (style != null && !style.isEmpty()) {
            // Prepend the base style to each span's style
            List<StyledSpan> result = new ArrayList<>(spans.size());
            for (StyledSpan span : spans) {
                String combinedStyle = style;
                if (span.style() != null && !span.style().isEmpty()) {
                    combinedStyle = style + " " + span.style();
                }
                result.add(new StyledSpan(span.text(), combinedStyle));
            }
            return result;
        }
        return spans;
    }

    private static String currentStyle(List<String> styleStack) {
        if (styleStack.isEmpty()) {
            return "";
        }
        return String.join(" ", styleStack);
    }

    private static boolean styleContains(List<String> stack, String style) {
        for (String s : stack) {
            if (s.equals(style)) {
                return true;
            }
        }
        return false;
    }
}
