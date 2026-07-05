package com.justnothing.richconsole.console;

import java.util.ArrayList;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.control.Control;
import com.justnothing.richconsole.segment.Segment;

/**
 * A renderable that updates screen content at specific positions.
 * Ported from rich/console.py ScreenUpdate.
 */
public class ScreenUpdate implements RichRenderable {

    private final List<List<Segment>> lines;
    private final int x;
    private final int y;

    public ScreenUpdate(List<List<Segment>> lines, int x, int y) {
        this.lines = lines;
        this.x = x;
        this.y = y;
    }

    public List<List<Segment>> getLines() {
        return lines;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Object> result = new ArrayList<>();
        int x = this.x;
        for (int i = 0; i < lines.size(); i++) {
            result.add(Control.moveTo(x, y + i).getSegment());
            result.addAll(lines.get(i));
        }
        return result;
    }
}
