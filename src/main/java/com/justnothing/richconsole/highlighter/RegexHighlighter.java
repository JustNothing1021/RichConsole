package com.justnothing.richconsole.highlighter;

import java.util.List;

import com.justnothing.richconsole.text.Text;

/**
 * Applies highlighting from a list of regular expressions.
 * Ported from rich/highlighter.py RegexHighlighter.
 *
 * Subclasses should define {@link #getHighlights()} returning regex patterns,
 * and optionally {@link #getBaseStyle()} returning a style prefix like "repr."
 * or "json.".
 */
public abstract class RegexHighlighter extends Highlighter {

    /**
     * Returns the list of regex patterns to highlight.
     */
    public abstract List<String> getHighlights();

    /**
     * Returns the base style prefix for named group styles.
     * For example, "repr." will produce styles like "repr.boolTrue".
     * Return null or empty string for no prefix.
     */
    public String getBaseStyle() {
        return "";
    }

    @Override
    public void highlight(Text text) {
        String stylePrefix = getBaseStyle();
        for (String pattern : getHighlights()) {
            text.highlightRegex(pattern, null, stylePrefix.isEmpty() ? null : stylePrefix);
        }
    }
}
