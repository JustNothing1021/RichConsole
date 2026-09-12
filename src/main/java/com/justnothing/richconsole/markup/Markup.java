package com.justnothing.richconsole.markup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Regex for matching emoji codes like :smile:, :smile-emoji:, :smile-text:.
     * Ported from rich/_emoji_replace.py.
     */
    private static final Pattern RE_EMOJI = Pattern.compile(":(\\S*?)(?:(?:\\-)(emoji|text))?:");

    private static final String EMOJI_VARIANT_TEXT = "\uFE0E";
    private static final String EMOJI_VARIANT_EMOJI = "\uFE0F";

    /**
     * Regex for marking escape patterns. Matches (zero or more backslashes) + (a markup tag).
     * Ported from rich/markup.py escape().
     */
    private static final Pattern RE_ESCAPE = Pattern.compile("(\\\\*)(\\[[a-z#/@][^\\]]*?])");

    /**
     * Escape square brackets in a string so they are not interpreted as markup.
     * Ported from rich/markup.py escape().
     *
     * <p>Only escapes patterns that look like markup tags, i.e., text in square brackets
     * beginning with a letter, '#', '/', or '@'. This avoids double-escaping
     * already-escaped brackets.
     *
     * @param markup the markup string to escape
     * @return the escaped string with markup tags escaped
     */
    public static String escape(String markup) {
        if (markup == null) {
            return "";
        }
        Matcher matcher = RE_ESCAPE.matcher(markup);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String backslashes = matcher.group(1); // already-escaped backslashes
            String text = matcher.group(2);        // the [tag] itself
            // Double the backslashes and prepend a backslash before the tag
            matcher.appendReplacement(sb, Matcher.quoteReplacement(backslashes + backslashes + "\\" + text));
        }
        matcher.appendTail(sb);
        String result = sb.toString();
        // Handle trailing backslash: if the string ends with a single backslash (odd count),
        // double it so it's not interpreted as escaping the next character
        if (result.endsWith("\\") && !result.endsWith("\\\\")) {
            result = result + "\\";
        }
        return result;
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
     * @param emoji  whether to process emoji (:code: → Unicode emoji)
     * @return a list of StyledSpan objects
     */
    public static List<StyledSpan> render(String markup, String style, boolean emoji) {
        // Apply emoji replacement before parsing markup (matching Python rich's behavior)
        String processedMarkup = emoji ? emojiReplace(markup) : markup;
        List<StyledSpan> spans = parse(processedMarkup);
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

    /**
     * Replace emoji codes like :smile: with Unicode emoji characters.
     * Ported from rich/_emoji_replace.py.
     *
     * @param text the text containing emoji codes
     * @return the text with emoji codes replaced
     */
    static String emojiReplace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = RE_EMOJI.matcher(text);
        while (matcher.find()) {
            String emojiCode = matcher.group(0);
            String emojiName = matcher.group(1);
            String variant = matcher.group(2);
            String emojiChar = EmojiCodes.get(emojiName != null ? emojiName.toLowerCase() : "");
            if (emojiChar != null) {
                String replacement = emojiChar;
                if ("text".equals(variant)) {
                    replacement = emojiChar + EMOJI_VARIANT_TEXT;
                } else if ("emoji".equals(variant)) {
                    replacement = emojiChar + EMOJI_VARIANT_EMOJI;
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(emojiCode));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
