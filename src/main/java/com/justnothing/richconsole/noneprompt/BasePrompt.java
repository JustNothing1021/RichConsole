package com.justnothing.richconsole.noneprompt;

import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.jansi.Ansi;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Abstract base class for interactive prompts.
 * Ported from noneprompt/BasePrompt.
 *
 * <p>Provides the core event loop: enter raw mode → render → read key →
 * update state → clear & re-render → return result.</p>
 *
 * @param <R> the result type of this prompt
 */
public abstract class BasePrompt<R> {

    // Operation name constants (resolved from KeyMap bindings)
    protected static final String OP_UP = "up";
    protected static final String OP_DOWN = "down";
    protected static final String OP_LEFT = "left";
    protected static final String OP_RIGHT = "right";
    protected static final String OP_ENTER = "enter";
    protected static final String OP_ESCAPE = "escape";
    protected static final String OP_BACKSPACE = "backspace";
    protected static final String OP_DELETE = "delete";
    protected static final String OP_HOME = "home";
    protected static final String OP_END = "end";
    protected static final String OP_PGUP = "page-up";
    protected static final String OP_PGDN = "page-down";
    protected static final String OP_SPACE = "space";
    protected static final String OP_TAB = "tab";
    protected static final String OP_CTRL_C = "ctrl-c";
    protected static final String OP_CTRL_Q = "ctrl-q";

    /** Shared KeyMap that maps raw key sequences to operation name strings. */
    private static final KeyMap<String> KEY_MAP = createKeyMap();

    private static KeyMap<String> createKeyMap() {
        KeyMap<String> km = new KeyMap<>();
        // Arrow keys (CSI and application keypad forms)
        km.bind(OP_UP, "\033[A");
        km.bind(OP_UP, "\033OA");
        km.bind(OP_DOWN, "\033[B");
        km.bind(OP_DOWN, "\033OB");
        km.bind(OP_RIGHT, "\033[C");
        km.bind(OP_RIGHT, "\033OC");
        km.bind(OP_LEFT, "\033[D");
        km.bind(OP_LEFT, "\033OD");
        // Home / End
        km.bind(OP_HOME, "\033[1~");
        km.bind(OP_HOME, "\033[H");
        km.bind(OP_END, "\033[4~");
        km.bind(OP_END, "\033[F");
        // Delete, Page Up, Page Down
        km.bind(OP_DELETE, "\033[3~");
        km.bind(OP_PGUP, "\033[5~");
        km.bind(OP_PGDN, "\033[6~");
        // Enter
        km.bind(OP_ENTER, "\r");
        km.bind(OP_ENTER, "\n");
        // Backspace (DEL = 0x7f, BS = 0x08)
        km.bind(OP_BACKSPACE, "\177");
        km.bind(OP_BACKSPACE, "\010");
        // Standalone Escape
        km.bind(OP_ESCAPE, "\033");
        // Space — bound AFTER the printable loop to avoid being overwritten
        // Tab
        km.bind(OP_TAB, "\t");
        // Ctrl+C, Ctrl+Q
        km.bind(OP_CTRL_C, "\003");
        km.bind(OP_CTRL_Q, "\021");
        // Printable ASCII characters — each character resolves to itself as the operation name
        // Start from 33 (!) to avoid overwriting space (32) which has its own binding
        for (char c = 33; c <= 126; c++) {
            km.bind(String.valueOf(c), Character.toString(c));
        }
        // Space must be bound AFTER the printable loop to take precedence
        km.bind(OP_SPACE, " ");
        km.setAmbiguousTimeout(10);
        return km;
    }

    protected final Console console;
    protected final Terminal terminal;
    private final BindingReader bindingReader;
    private Attributes savedAttributes;
    private int renderedLines = 0;

    public BasePrompt(Console console) {
        this(console, null);
    }

    public BasePrompt(Console console, PromptStyle promptStyle) {
        this.console = console;
        this.terminal = console.getTerminal();
        this.bindingReader = terminal != null ? new BindingReader(terminal.reader()) : null;
        this.promptStyle = promptStyle != null ? promptStyle : new PromptStyle();
    }

    /**
     * Run the prompt and return the result.
     *
     * @return the user's selection
     * @throws CancelledException if the user cancels (Ctrl+C/Q)
     */
    public R prompt() throws CancelledException {
        if (terminal == null) {
            throw new CancelledException("No terminal available for interactive prompt");
        }

        enterRawMode();
        try {
            reset();
            R result = runLoop();
            // Move to a new line after prompt completes
            writeRaw("\r\n");
            terminal.writer().flush();
            return result;
        } finally {
            exitRawMode();
        }
    }

    // =========================================================================
    // Abstract methods — subclasses must implement
    // =========================================================================

    /**
     * Reset the prompt state (called before each prompt() invocation).
     */
    protected abstract void reset();

    /**
     * Render the current prompt state to a list of Segments.
     * Each inner list represents one line.
     */
    protected abstract List<List<Segment>> render();

    /**
     * Handle a key operation. Return true if the prompt should finish.
     *
     * @param op the operation name (use OP_* constants, or a single-char string for printable input)
     * @return true to finish the prompt, false to continue
     */
    protected abstract boolean handleKey(String op);

    /**
     * Get the final result after the prompt finishes.
     */
    protected abstract R getResult();

    // =========================================================================
    // Event loop
    // =========================================================================

    private R runLoop() throws CancelledException {
        // Initial render
        redraw();

        while (true) {
            String op = readKey();
            if (op.equals(OP_CTRL_C) || op.equals(OP_CTRL_Q)) {
                throw new CancelledException();
            }

            boolean done = handleKey(op);
            if (done) {
                // Final render (showing the answered state)
                clearRendered();
                renderedLines = 0;
                redraw();
                return getResult();
            }

            // Re-render
            clearRendered();
            renderedLines = 0;
            redraw();
        }
    }

    // =========================================================================
    // Terminal raw mode
    // =========================================================================

    private void enterRawMode() {
        // 使用 terminal.enterRawMode() 而非手动 setAttributes()，
        // 这样 RemoteServerTerminal 的重写方法才能正确触发，
        // 向客户端发送 "enterRawMode" RPC 信号以同步 raw mode 状态。
        // JLine Terminal.enterRawMode() 内部实现与之前手动设置的属性完全一致。
        savedAttributes = terminal.enterRawMode();
    }

    private void exitRawMode() {
        if (savedAttributes != null) {
            terminal.setAttributes(savedAttributes);
            savedAttributes = null;
        }
    }

    // =========================================================================
    // Key reading
    // =========================================================================

    private String readKey() throws CancelledException {
        String op = bindingReader.readBinding(KEY_MAP);
        if (op == null) {
            throw new CancelledException("End of input");
        }
        return op;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    protected void redraw() {
        List<List<Segment>> lines = render();
        // Calculate visual lines (accounting for terminal wrapping)
        int termWidth = getTerminalWidth();
        int visualLineCount = 0;
        for (List<Segment> line : lines) {
            int lineWidth = 0;
            for (Segment seg : line) {
                lineWidth += seg.getText().length();
            }
            // Each logical line takes at least 1 visual line; wrapping adds more
            visualLineCount += Math.max(1, (lineWidth + termWidth - 1) / termWidth);
        }
        // Clear from cursor to end of screen (handles shorter new content)
        writeRaw(new Ansi().eraseScreen(Ansi.Erase.FORWARD).toString());
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                writeRaw("\r\n");
            }
            List<Segment> line = lines.get(i);
            String ansi = segmentsToAnsi(line);
            writeRaw(ansi);
        }
        // Move cursor to beginning of next line after content
        writeRaw("\r\n");
        // renderedLines = visual line count + 1 (for the cursor line after \r\n)
        renderedLines = visualLineCount + 1;
        try {
            terminal.writer().flush();
        } catch (Exception ignored) {}
    }

    protected void clearRendered() {
        if (renderedLines <= 0) return;
        // Cursor is at the beginning of the line below the content after redraw().
        // Move up to the first rendered line, then clear to end of screen.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < renderedLines - 1; i++) {
            sb.append(new Ansi().cursorUp(1).toString());
        }
        sb.append("\r");
        sb.append(new Ansi().eraseScreen(Ansi.Erase.FORWARD).toString());
        writeRaw(sb.toString());
        try {
            terminal.writer().flush();
        } catch (Exception ignored) {}
    }

    private int getTerminalWidth() {
        if (console != null) {
            return console.getWidth();
        }
        return 80;
    }

    /**
     * Convert a list of Segments to an ANSI string for direct terminal output.
     */
    protected String segmentsToAnsi(List<Segment> segments) {
        StringBuilder output = new StringBuilder();
        for (Segment segment : segments) {
            String text = segment.getText();
            Style style = segment.getStyle();
            if (style != null && !style.isNull()) {
                output.append(style.render(text, console.getColorSystemEnum(), false));
            } else {
                output.append(text);
            }
        }
        return output.toString();
    }

    protected void writeRaw(String text) {
        try {
            terminal.writer().write(text);
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // Style helpers
    // =========================================================================

    /** Prompt visual style configuration. */
    public static class PromptStyle {
        public Style questionMark = Style.parse("bold #673AB7");
        public Style question = Style.parse("bold");
        public Style annotation = Style.parse("#7F8C8D");
        public Style answer = Style.parse("#FF9D00");
        public Style selected = Style.parse("green");
        public Style error = Style.parse("bold white on red");
        public Style pointer = Style.parse("bold");
        public Style checkboxSign = Style.parse("green");

        public PromptStyle questionMark(Style s) { this.questionMark = s; return this; }
        public PromptStyle question(Style s) { this.question = s; return this; }
        public PromptStyle annotation(Style s) { this.annotation = s; return this; }
        public PromptStyle answer(Style s) { this.answer = s; return this; }
        public PromptStyle selected(Style s) { this.selected = s; return this; }
        public PromptStyle error(Style s) { this.error = s; return this; }
        public PromptStyle pointer(Style s) { this.pointer = s; return this; }
        public PromptStyle checkboxSign(Style s) { this.checkboxSign = s; return this; }
    }

    protected final PromptStyle promptStyle;

    /**
     * Create the noneprompt default style for question mark.
     */
    protected Style questionMarkStyle() {
        return promptStyle.questionMark;
    }

    /**
     * Create the noneprompt default style for question text.
     */
    protected Style questionStyle() {
        return promptStyle.question;
    }

    /**
     * Create the noneprompt default style for annotation text.
     */
    protected Style annotationStyle() {
        return promptStyle.annotation;
    }

    /**
     * Create the noneprompt default style for answer text.
     */
    protected Style answerStyle() {
        return promptStyle.answer;
    }

    /**
     * Create the noneprompt default style for selected item.
     */
    protected Style selectedStyle() {
        return promptStyle.selected;
    }

    /**
     * Create the noneprompt default style for error message.
     */
    protected Style errorStyle() {
        return promptStyle.error;
    }

    /**
     * Create the noneprompt default style for pointer.
     */
    protected Style pointerStyle() {
        return promptStyle.pointer;
    }
}
