package com.justnothing.richconsole.highlighter;

import com.justnothing.richconsole.text.Text;

/**
 * Abstract base class for highlighters.
 * Ported from rich/highlighter.py.
 */
public abstract class Highlighter {

    /**
     * Highlight a str or Text instance.
     *
     * @param text the text to highlight
     * @return a Text instance with highlighting applied
     */
    public Text apply(Object text) {
        Text highlightText;
        if (text instanceof String) {
            highlightText = new Text((String) text);
        } else if (text instanceof Text) {
            highlightText = ((Text) text).copy();
        } else {
            throw new IllegalArgumentException("str or Text instance required, not " + text);
        }
        highlight(highlightText);
        return highlightText;
    }

    /**
     * Apply highlighting in place to text.
     *
     * @param text a Text object to highlight
     */
    public abstract void highlight(Text text);
}
