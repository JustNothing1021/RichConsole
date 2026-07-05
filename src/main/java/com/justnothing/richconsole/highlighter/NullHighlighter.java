package com.justnothing.richconsole.highlighter;

import com.justnothing.richconsole.text.Text;

/**
 * A highlighter that doesn't highlight.
 * Used to disable highlighting entirely.
 */
public class NullHighlighter extends Highlighter {

    @Override
    public void highlight(Text text) {
        // Nothing to do
    }
}
