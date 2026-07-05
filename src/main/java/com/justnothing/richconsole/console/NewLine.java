package com.justnothing.richconsole.console;

import java.util.Collections;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.segment.Segment;

/**
 * A renderable that inserts newlines.
 * Ported from rich/console.py NewLine.
 */
public class NewLine implements RichRenderable {

    private final int count;

    public NewLine(int count) {
        this.count = count;
    }

    public NewLine() {
        this(1);
    }

    public int getCount() {
        return count;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append('\n');
        }
        return Collections.singletonList(new Segment(sb.toString()));
    }
}
