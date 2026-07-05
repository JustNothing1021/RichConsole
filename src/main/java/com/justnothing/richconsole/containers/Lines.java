package com.justnothing.richconsole.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.cells.Cells;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.text.Text;

/**
 * A container for lines of renderables.
 * Ported from rich/containers.py Lines.
 */
public class Lines implements RichRenderable, Iterable<Object> {
    public static final String LEFT = "left";
    public static final String CENTER = "center";
    public static final String RIGHT = "right";


    private final List<Object> lines;

    public Lines() {
        this.lines = new ArrayList<>();
    }

    public Lines(List<Object> lines) {
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public void append(Object line) {
        lines.add(line);
    }

    public void extend(List<Object> newLines) {
        if (newLines != null) {
            lines.addAll(newLines);
        }
    }

    public Object pop() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Lines is empty");
        }
        return lines.remove(lines.size() - 1);
    }

    public Object get(int index) {
        return lines.get(index);
    }

    public void set(int index, Object line) {
        lines.set(index, line);
    }

    public int size() {
        return lines.size();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public List<Object> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Justify lines within the given width.
     * Ported from rich/containers.py Lines.justify().
     *
     * @param width    the width to justify to
     * @param method   justification method ("left", "center", "right")
     * @param overflow overflow method ("crop", "fold", "ellipsis")
     */
    public void justify(int width, String method, String overflow) {
        if (LEFT.equals(method)) {
            for (Object line : lines) {
                if (line instanceof Text) {
                    ((Text) line).truncate(width, overflow, true);
                }
            }
        } else if (CENTER.equals(method)) {
            for (Object line : lines) {
                if (line instanceof Text text) {
                    text.rstrip();
                    text.truncate(width, overflow, false);
                    int cellLen = Cells.cellLen(text.getPlain());
                    text.padLeft((width - cellLen) / 2);
                    cellLen = Cells.cellLen(text.getPlain());
                    text.padRight(width - cellLen);
                }
            }
        } else if (RIGHT.equals(method)) {
            for (Object line : lines) {
                if (line instanceof Text text) {
                    text.rstrip();
                    text.truncate(width, overflow, false);
                    int cellLen = Cells.cellLen(text.getPlain());
                    text.padLeft(width - cellLen);
                }
            }
        }
    }

    /**
     * Justify lines within the given width using default overflow "fold".
     *
     * @param width  the width to justify to
     * @param method justification method ("left", "center", "right")
     */
    public void justify(int width, String method) {
        justify(width, method, "fold");
    }

    @Override
    public Iterator<Object> iterator() {
        return lines.iterator();
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Object> result = new ArrayList<>();
        for (Object line : lines) {
            if (line instanceof RichRenderable) {
                Iterable<?> rendered = ((RichRenderable) line).richConsole(console, options);
                for (Object item : rendered) {
                    result.add(item);
                }
            } else {
                result.add(line);
            }
        }
        return result;
    }
}
