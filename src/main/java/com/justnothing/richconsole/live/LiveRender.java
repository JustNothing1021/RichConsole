package com.justnothing.richconsole.live;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jline.jansi.Ansi;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.ControlCode;
import com.justnothing.richconsole.segment.ControlType;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * A renderable wrapper for Live display that tracks its rendered shape.
 * Ported from rich/live_render.py LiveRender class.
 *
 * <p>When rendered, it updates its internal shape (width, height) tracking
 * how many lines were rendered, allowing the Live class to correctly
 * position the cursor for subsequent refreshes.</p>
 */
public class LiveRender implements RichRenderable {

    private Object renderable;
    private final Style style;
    private String verticalOverflow;
    private int[] shape = null; // [width, height]

    /**
     * Create a LiveRender with a renderable and optional style.
     *
     * @param renderable       the renderable to wrap
     * @param style            optional style to apply
     * @param verticalOverflow how to handle overflow ("ellipsis", "crop", "visible")
     */
    public LiveRender(Object renderable, Style style, String verticalOverflow) {
        this.renderable = renderable;
        this.style = style;
        this.verticalOverflow = verticalOverflow != null ? verticalOverflow : "ellipsis";
    }

    /**
     * Create a LiveRender with just a renderable.
     */
    public LiveRender(Object renderable) {
        this(renderable, null, "ellipsis");
    }

    /**
     * Get the last rendered height.
     *
     * @return height in lines, or 0 if nothing was rendered
     */
    public int getLastRenderHeight() {
        if (shape == null) return 0;
        return shape[1];
    }

    /**
     * Get the last rendered width.
     *
     * @return width in cells, or 0 if nothing was rendered
     */
    public int getLastRenderWidth() {
        if (shape == null) return 0;
        return shape[0];
    }

    /**
     * Set a new renderable.
     */
    public void setRenderable(Object renderable) {
        this.renderable = renderable;
    }

    /**
     * Set the vertical overflow mode.
     */
    public void setVerticalOverflow(String verticalOverflow) {
        this.verticalOverflow = verticalOverflow;
    }

    /**
     * Get control codes to move cursor to beginning of live render.
     * Equivalent to Python rich's position_cursor().
     *
     * @return list of control segments
     */
    public List<Segment> positionCursor() {
        List<Segment> segments = new ArrayList<>();
        if (shape != null && shape[1] > 0) {
            int height = shape[1];
            Ansi ansi = new Ansi();
            // Carriage return + erase entire line
            ansi.a("\r").eraseLine(Ansi.Erase.ALL);
            // For each additional line: cursor up + erase line
            for (int i = 0; i < height - 1; i++) {
                ansi.cursorUp(1).eraseLine(Ansi.Erase.ALL);
            }
            // Mark as control segment so adjustLineLength doesn't count
            // the ANSI escape characters toward the line width.
            // Matches Python rich's Segment.control() behavior.
            List<ControlCode> codes = Collections.singletonList(
                    new ControlCode(ControlType.CARRIAGE_RETURN));
            segments.add(new Segment(ansi.toString(), null, codes));
        }
        return segments;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        // Render the wrapped renderable
        List<List<Segment>> lines = console.renderLines(renderable, options, style, false, false);

        // Calculate shape
        int width = 0;
        int height = lines.size();
        for (List<Segment> line : lines) {
            int lineWidth = 0;
            for (Segment seg : line) {
                lineWidth += seg.getText().length(); // simplified, should use cellLen
            }
            width = Math.max(width, lineWidth);
        }
        shape = new int[]{width, height};

        // Handle vertical overflow
        Integer maxHeight = options.getHeight();
        if (maxHeight != null && height > maxHeight) {
            if ("crop".equals(verticalOverflow)) {
                lines = lines.subList(0, maxHeight);
                shape[1] = maxHeight;
            } else if ("ellipsis".equals(verticalOverflow)) {
                List<List<Segment>> cropped = new ArrayList<>(lines.subList(0, maxHeight - 1));
                List<Segment> ellipsisLine = new ArrayList<>();
                ellipsisLine.add(new Segment("...", null));
                cropped.add(ellipsisLine);
                lines = cropped;
                shape[1] = maxHeight;
            }
        }

        // Flatten lines into segments with newlines
        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            result.addAll(lines.get(i));
            if (i < lines.size() - 1) {
                result.add(Segment.line());
            }
        }
        return result;
    }
}