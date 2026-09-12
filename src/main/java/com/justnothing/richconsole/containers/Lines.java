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
    public static final String FULL = "full";


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
        } else if (FULL.equals(method)) {
            // Full justify: distribute extra spaces between words for all lines except the last.
            // Ported from rich/containers.py Lines.justify() "full" branch.
            for (int lineIdx = 0; lineIdx < lines.size() - 1; lineIdx++) {
                Object lineObj = lines.get(lineIdx);
                if (lineObj instanceof Text text) {
                    text.rstrip();
                    String plain = text.getPlain();
                    if (plain.isEmpty()) continue;
                    String[] words = plain.split(" ");
                    if (words.length <= 1) {
                        text.truncate(width, overflow, true);
                        continue;
                    }
                    int totalWordLen = 0;
                    for (String w : words) {
                        totalWordLen += Cells.cellLen(w);
                    }
                    int gaps = words.length - 1;
                    int totalSpaces = width - totalWordLen;
                    if (totalSpaces <= gaps) {
                        text.truncate(width, overflow, true);
                        continue;
                    }
                    int baseSpaces = totalSpaces / gaps;
                    int extraSpaces = totalSpaces % gaps;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < words.length; i++) {
                        sb.append(words[i]);
                        if (i < gaps) {
                            int spaces = baseSpaces + (i < extraSpaces ? 1 : 0);
                            sb.append(" ".repeat(spaces));
                        }
                    }
                    Text newText = text.blankCopy(sb.toString());
                    lines.set(lineIdx, newText);
                }
            }
            // Last line: left-align
            if (!lines.isEmpty()) {
                Object lastLine = lines.get(lines.size() - 1);
                if (lastLine instanceof Text text) {
                    text.truncate(width, overflow, true);
                }
            }
        }
    }

    /**
     * Justify lines within the given width using default overflow "fold".
     *
     * @param width  the width to justify to
     * @param method justification method ("left", "center", "right", "full")
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
