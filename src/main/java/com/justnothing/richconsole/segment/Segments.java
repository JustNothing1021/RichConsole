package com.justnothing.richconsole.segment;

import java.util.Collections;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;

/**
 * A simple wrapper around a list of segments.
 * Ported from rich/segment.py Segments.
 */
public class Segments implements RichRenderable {

    private final List<Segment> segments;
    private final boolean newLines;

    public Segments(List<Segment> segments) {
        this(segments, false);
    }

    public Segments(List<Segment> segments, boolean newLines) {
        this.segments = segments != null
                ? Collections.unmodifiableList(segments)
                : Collections.emptyList();
        this.newLines = newLines;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public boolean isNewLines() {
        return newLines;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        if (newLines) {
            return Segment.splitAndCropLines(segments, 80, null, false, true);
        }
        return segments;
    }
}
