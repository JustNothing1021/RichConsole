package com.justnothing.richconsole.segment;

import java.util.Collections;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;

/**
 * A renderable that wraps pre-split lines of segments.
 * Ported from rich/segment.py SegmentLines.
 */
public class SegmentLines implements RichRenderable {

    private final List<List<Segment>> lines;
    private final boolean newLines;

    public SegmentLines(List<List<Segment>> lines) {
        this(lines, false);
    }

    public SegmentLines(List<List<Segment>> lines, boolean newLines) {
        this.lines = lines != null
                ? Collections.unmodifiableList(lines)
                : Collections.emptyList();
        this.newLines = newLines;
    }

    public List<List<Segment>> getLines() {
        return lines;
    }

    public boolean isNewLines() {
        return newLines;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        if (newLines) {
            // Already split into lines; yield them with new-line segments
            List<Segment> result = new java.util.ArrayList<>();
            for (List<Segment> line : lines) {
                result.addAll(line);
                result.add(Segment.line());
            }
            return result;
        }
        // Flatten all lines into a single iterable
        List<Segment> result = new java.util.ArrayList<>();
        for (List<Segment> line : lines) {
            result.addAll(line);
        }
        return result;
    }
}
