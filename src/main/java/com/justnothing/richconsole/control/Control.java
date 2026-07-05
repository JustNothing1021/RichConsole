package com.justnothing.richconsole.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.jansi.Ansi;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.ControlCode;
import com.justnothing.richconsole.segment.ControlType;
import com.justnothing.richconsole.segment.Segment;

/**
 * Terminal control sequences.
 * Ported from rich/control.py.
 *
 * <p>Generates ANSI escape sequences for terminal control operations.
 * Uses JLine's {@link Ansi} builder class for generating sequences,
 * ensuring proper and consistent ANSI code generation.</p>
 */
public class Control implements RichRenderable {

    /** CSI: Show cursor (DECTCEM set). */
    private static final String CSI_SHOW_CURSOR = "\u001b[?25h";
    /** CSI: Hide cursor (DECTCEM reset). */
    private static final String CSI_HIDE_CURSOR = "\u001b[?25l";
    /** CSI: Enable alternate screen buffer. */
    private static final String CSI_ENABLE_ALT_SCREEN = "\u001b[?1049h";
    /** CSI: Disable alternate screen buffer. */
    private static final String CSI_DISABLE_ALT_SCREEN = "\u001b[?1049l";
    /** OSC: Window title prefix. */
    private static final String OSC_TITLE_PREFIX = "\u001b]0;";
    /** BEL character (used as OSC terminator). */
    private static final String OSC_BEL = "\u0007";

    /**
     * Regex pattern for stripping control codes from text.
     */
    private static final Pattern STRIP_CONTROL_CODES_PATTERN =
            Pattern.compile("\u001B\\[\\??\\d+[A-Za-z]|\u001B]\\d+;[^\u0007]*\u0007|\u001B\\[[A-Za-z]");

    /**
     * Regex pattern for control escape sequences (for escaping in text).
     */
    private static final Pattern CONTROL_ESCAPE_PATTERN =
            Pattern.compile("([\u001b\u0007])");

    /**
     * Mapping from ControlType value to ANSI code generator function.
     * Uses JLine's Ansi builder where possible.
     */
    private static final Map<Integer, Function<int[], String>> ANSI_FORMAT;

    static {
        Map<Integer, Function<int[], String>> ansiMap = new HashMap<>();
        ansiMap.put(ControlType.BELL.getValue(), params -> OSC_BEL);
        ansiMap.put(ControlType.CARRIAGE_RETURN.getValue(), params -> "\r");
        ansiMap.put(ControlType.HOME.getValue(), params ->
                new Ansi().cursor(1, 1).toString());
        ansiMap.put(ControlType.CLEAR.getValue(), params ->
                new Ansi().eraseScreen(Ansi.Erase.ALL).toString());
        ansiMap.put(ControlType.SHOW_CURSOR.getValue(), params -> CSI_SHOW_CURSOR);
        ansiMap.put(ControlType.HIDE_CURSOR.getValue(), params -> CSI_HIDE_CURSOR);
        ansiMap.put(ControlType.ENABLE_ALT_SCREEN.getValue(), params -> CSI_ENABLE_ALT_SCREEN);
        ansiMap.put(ControlType.DISABLE_ALT_SCREEN.getValue(), params -> CSI_DISABLE_ALT_SCREEN);
        ansiMap.put(ControlType.CURSOR_UP.getValue(), params ->
                new Ansi().cursorUp(paramInt(params, 0)).toString());
        ansiMap.put(ControlType.CURSOR_DOWN.getValue(), params ->
                new Ansi().cursorDown(paramInt(params, 0)).toString());
        ansiMap.put(ControlType.CURSOR_FORWARD.getValue(), params ->
                new Ansi().cursorRight(paramInt(params, 0)).toString());
        ansiMap.put(ControlType.CURSOR_BACKWARD.getValue(), params ->
                new Ansi().cursorLeft(paramInt(params, 0)).toString());
        ansiMap.put(ControlType.CURSOR_MOVE_TO_COLUMN.getValue(), params ->
                new Ansi().cursorToColumn(paramInt(params, 0)).toString());
        ansiMap.put(ControlType.CURSOR_MOVE_TO.getValue(), params ->
                new Ansi().cursor(paramInt(params, 0), paramInt(params, 1)).toString());
        ansiMap.put(ControlType.ERASE_IN_LINE.getValue(), params ->
                new Ansi().eraseLine(toErase(paramInt(params, 0))).toString());
        ansiMap.put(ControlType.SET_WINDOW_TITLE.getValue(), params -> {
            // OSC sequence: ESC ] 0 ; title BEL
            return OSC_TITLE_PREFIX + (params.length > 0 ? params[0] : "") + OSC_BEL;
        });
        ANSI_FORMAT = Collections.unmodifiableMap(ansiMap);
    }

    private static int paramInt(int[] params, int index) {
        if (params != null && index < params.length) {
            return params[index];
        }
        return 1;
    }

    private static Ansi.Erase toErase(int mode) {
        return switch (mode) {
            case 1 -> Ansi.Erase.BACKWARD;
            case 2 -> Ansi.Erase.ALL;
            default -> Ansi.Erase.FORWARD;
        };
    }

    private final Segment segment;

    private Control(Segment segment) {
        this.segment = segment;
    }

    /**
     * Get the underlying segment.
     */
    public Segment getSegment() {
        return segment;
    }

    /**
     * Render this control as a string (the ANSI escape sequence).
     */
    @Override
    public String toString() {
        return segment.getText();
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        return Collections.singletonList(segment);
    }

    // --- Static factory methods ---

    /**
     * Create a bell control.
     */
    public static Control bell() {
        return new Control(makeSegment(ControlType.BELL));
    }

    /**
     * Create a home control (move cursor to home position).
     */
    public static Control home() {
        return new Control(makeSegment(ControlType.HOME));
    }

    /**
     * Move cursor up by n lines.
     */
    public static Control moveUp(int n) {
        return new Control(makeSegment(ControlType.CURSOR_UP, n));
    }

    /**
     * Move cursor down by n lines.
     */
    public static Control moveDown(int n) {
        return new Control(makeSegment(ControlType.CURSOR_DOWN, n));
    }

    /**
     * Clear lines above the current position.
     * Moves cursor to beginning of current line, clears it,
     * then moves up and clears each previous line.
     *
     * <p>Equivalent to Python rich's LiveRender.position_cursor().</p>
     *
     * @param height number of lines to clear (including current line)
     * @return Control that clears the specified number of lines
     */
    public static Control clearLines(int height) {
        if (height <= 0) {
            return new Control(new Segment("", null, Collections.emptyList()));
        }
        StringBuilder sb = new StringBuilder();
        // \r - carriage return (move to beginning of line)
        sb.append("\r");
        // \x1b[2K - erase entire line
        sb.append(new Ansi().eraseLine(Ansi.Erase.ALL).toString());
        // For each additional line: cursor up + erase line
        for (int i = 0; i < height - 1; i++) {
            sb.append(new Ansi().cursorUp(1).toString());
            sb.append(new Ansi().eraseLine(Ansi.Erase.ALL).toString());
        }
        // Mark as control segment so adjustLineLength doesn't count
        // the ANSI escape characters toward the line width.
        List<ControlCode> codes = Collections.singletonList(
                new ControlCode(ControlType.CARRIAGE_RETURN));
        return new Control(new Segment(sb.toString(), null, codes));
    }

    /**
     * Move cursor by (x, y) where x is vertical and y is horizontal.
     */
    public static Control move(int x, int y) {
        if (x != 0) {
            if (x < 0) {
                return new Control(makeSegment(ControlType.CURSOR_UP, -x));
            }
            return new Control(makeSegment(ControlType.CURSOR_DOWN, x));
        }
        if (y < 0) {
            return new Control(makeSegment(ControlType.CURSOR_BACKWARD, -y));
        }
        return new Control(makeSegment(ControlType.CURSOR_FORWARD, y));
    }

    /**
     * Move cursor to a specific column.
     */
    public static Control moveToColumn(int column) {
        return new Control(makeSegment(ControlType.CURSOR_MOVE_TO_COLUMN, column));
    }

    /**
     * Move cursor to a specific position (row, column).
     */
    public static Control moveTo(int x, int y) {
        return new Control(makeSegment(ControlType.CURSOR_MOVE_TO, x, y));
    }

    /**
     * Create a clear screen control.
     */
    public static Control clear() {
        return new Control(makeSegment(ControlType.CLEAR));
    }

    /**
     * Erase in line.
     * @param mode 0=cursor to end, 1=start to cursor, 2=entire line
     */
    public static Control eraseInLine(int mode) {
        return new Control(makeSegment(ControlType.ERASE_IN_LINE, mode));
    }

    /**
     * Show or hide the cursor.
     */
    public static Control showCursor(boolean show) {
        if (show) {
            return new Control(makeSegment(ControlType.SHOW_CURSOR));
        }
        return new Control(makeSegment(ControlType.HIDE_CURSOR));
    }

    /**
     * Enable or disable alternate screen.
     */
    public static Control altScreen(boolean enable) {
        if (enable) {
            return new Control(makeSegment(ControlType.ENABLE_ALT_SCREEN));
        }
        return new Control(makeSegment(ControlType.DISABLE_ALT_SCREEN));
    }

    /**
     * Set the window title.
     * Uses OSC (Operating System Command) sequence, not CSI.
     */
    public static Control title(String title) {
        // OSC sequence: ESC ] 0 ; title BEL — not a CSI sequence, so we build it manually
        String text = OSC_TITLE_PREFIX + title + OSC_BEL;
        List<ControlCode> codes = Collections.singletonList(
                new ControlCode(ControlType.SET_WINDOW_TITLE));
        return new Control(new Segment(text, null, codes));
    }

    // --- Internal helpers ---

    private static Segment makeSegment(ControlType type, int... params) {
        Function<int[], String> formatter = ANSI_FORMAT.get(type.getValue());
        String text = formatter != null ? formatter.apply(params) : "";
        List<ControlCode> codes = Collections.singletonList(new ControlCode(type, params));
        return new Segment(text, null, codes);
    }

    // --- Static utility methods ---

    /**
     * Strip control codes from text.
     *
     * @param text text that may contain ANSI control sequences
     * @return text with control sequences removed
     */
    public static String stripControlCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return STRIP_CONTROL_CODES_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Escape control codes in text by replacing ESC and BEL characters
     * with their escaped representations.
     *
     * @param text text that may contain control characters
     * @return text with control characters escaped
     */
    public static String escapeControlCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher m = CONTROL_ESCAPE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            char ch = m.group(1).charAt(0);
            if (ch == '\u001b') {
                m.appendReplacement(sb, "\\\\u001b");
            } else if (ch == '\u0007') {
                m.appendReplacement(sb, "\\\\u0007");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
