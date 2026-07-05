package com.justnothing.richconsole.console;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.justnothing.richconsole.json.JSON;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.status.Status;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.justnothing.richconsole.highlighter.ReprHighlighter;
import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.color.ColorSystem;
import com.justnothing.richconsole.control.Control;
import com.justnothing.richconsole.errors.MissingStyle;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.log.LogRender;
import com.justnothing.richconsole.errors.NoAltScreen;
import com.justnothing.richconsole.errors.NotRenderableError;
import com.justnothing.richconsole.errors.StyleSyntaxError;
import com.justnothing.richconsole.markup.Markup;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.protocol.Protocol;
import com.justnothing.richconsole.region.Region;
import com.justnothing.richconsole.rule.Rule;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.styled.Styled;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;
import com.justnothing.richconsole.theme.Theme;
import com.justnothing.richconsole.theme.ThemeStack;
import com.justnothing.richconsole.traceback.Traceback;

/**
 * A high level console interface.
 * Ported from rich/console.py Console class.
 *
 * <p>Console is the core engine of Rich. It manages terminal detection, rendering,
 * buffering, theme stacks, and output. It supports printing rich content,
 * capturing output, recording for HTML/SVG export, and more.</p>
 */

public class Console {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final int JUPYTER_DEFAULT_COLUMNS = 115;
    private static final int JUPYTER_DEFAULT_LINES = 100;
    private static final int DEFAULT_WIDTH = 80;
    private static final int DEFAULT_HEIGHT = 25;
    private static final int MAX_WRITE = 32 * 1024 / 4; // 8K chars
    private static final int DEFAULT_TAB_SIZE = 8;

    private static final String COLOR_SYSTEM_AUTO = "auto";
    private static final String TERM_TYPE_DUMB = "dumb";
    private static final String TERM_TYPE_UNKNOWN = "unknown";
    private static final String DEFAULT_ENCODING = "utf-8";
    private static final String COLOR_SYSTEM_NAME_STANDARD = "standard";
    private static final String COLOR_SYSTEM_NAME_256 = "256";
    private static final String COLOR_SYSTEM_NAME_TRUECOLOR = "truecolor";
    private static final String COLOR_SYSTEM_NAME_WINDOWS = "windows";
    private static final String COLORTERM_TRUECOLOR = "truecolor";
    private static final String COLORTERM_24BIT = "24bit";
    private static final String OVERFLOW_IGNORE = "ignore";

    private static final Map<String, ColorSystem> COLOR_SYSTEMS;
    static {
        Map<String, ColorSystem> m = new LinkedHashMap<>();
        m.put(COLOR_SYSTEM_NAME_STANDARD, ColorSystem.STANDARD);
        m.put(COLOR_SYSTEM_NAME_256, ColorSystem.EIGHT_BIT);
        m.put(COLOR_SYSTEM_NAME_TRUECOLOR, ColorSystem.TRUECOLOR);
        m.put(COLOR_SYSTEM_NAME_WINDOWS, ColorSystem.WINDOWS);
        COLOR_SYSTEMS = Collections.unmodifiableMap(m);
    }

    private static final Map<ColorSystem, String> COLOR_SYSTEMS_NAMES;
    static {
        Map<ColorSystem, String> m = new LinkedHashMap<>();
        m.put(ColorSystem.STANDARD, COLOR_SYSTEM_NAME_STANDARD);
        m.put(ColorSystem.EIGHT_BIT, COLOR_SYSTEM_NAME_256);
        m.put(ColorSystem.TRUECOLOR, COLOR_SYSTEM_NAME_TRUECOLOR);
        m.put(ColorSystem.WINDOWS, COLOR_SYSTEM_NAME_WINDOWS);
        COLOR_SYSTEMS_NAMES = Collections.unmodifiableMap(m);
    }

    // =========================================================================
    // Instance fields
    // =========================================================================

    private final ColorSystem colorSystem;
    private final Boolean forceTerminal;
    private final boolean forceJupyter;
    private final Boolean forceInteractive;
    private final boolean softWrap;
    private final Theme theme; // used by ThemeStack constructor
    private Integer width;
    private Integer height;
    private final Object style;
    private final boolean noColor;
    private final int tabSize;
    private final boolean record;
    private final boolean markupEnabled;
    private final boolean emojiEnabled;
    private final boolean highlightEnabled;
    private final boolean quiet;
    private final boolean stderr;
    private final boolean legacyWindows;
    private final boolean safeBox;
    private final PrintStream outputFile;
    private final Terminal terminal; // JLine terminal, null if unavailable

    private final ReentrantLock lock = new ReentrantLock();
    private final ThemeStack themeStack;
    private final List<Segment> buffer = new ArrayList<>();
    private int bufferIndex = 0;
    private final List<Segment> recordBuffer = new ArrayList<>();
    private final ReentrantLock recordBufferLock = new ReentrantLock();
    private final List<RenderHook> renderHooks = new ArrayList<>();
    private final List<Live> liveStack = new ArrayList<>();
    private boolean isAltScreen = false;
    private final LogRender logRender;
    private final ReprHighlighter highlighter;
    // TODO: Use logTimeFormat in log() method for formatting the time column
    private final Object logTimeFormat;

    public static class ConsoleConfig {
        String colorSystem = null;
        Boolean forceTerminal = null;
        Boolean forceJupyter = null;
        Boolean forceInteractive = null;
        boolean softWrap = false;
        Theme theme = null;
        boolean stderr = false;
        PrintStream outputFile = null;
        boolean quiet = false;
        Integer width = null;
        Integer height = null;
        Object style = null;
        Boolean noColor = false;
        int tabSize = DEFAULT_TAB_SIZE;
        boolean record = false;
        boolean markup = true;
        boolean emoji = true;
        boolean highlight = true;
        Object logTimeFormat = null;
        Object highlighter = null;
        boolean safeBox = true;
        Boolean legacyWindows;
        Consumer<TerminalBuilder> terminalConfigurer = null;
        public ConsoleConfig() {
        }

        /**
         * Create and configure a ConsoleConfig via a consumer, then build it.
         */
        static ConsoleConfig configure(Consumer<ConsoleConfig> configurer) {
            ConsoleConfig cfg = new ConsoleConfig();
            configurer.accept(cfg);
            return cfg;
        }

        public ConsoleConfig withColorSystem(String colorSystem) {
            this.colorSystem = colorSystem;
            return this;
        }

        public ConsoleConfig withForceTerminal(Boolean forceTerminal) {
            this.forceTerminal = forceTerminal;
            return this;
        }

        public ConsoleConfig withForceJupyter(Boolean forceJupyter) {
            this.forceJupyter = forceJupyter;
                       return this;
        }

        public ConsoleConfig withForceInteractive(Boolean forceInteractive) {
            this.forceInteractive = forceInteractive;
            return this;
        }

        public ConsoleConfig withSoftWrap(boolean softWrap) {
            this.softWrap = softWrap;
            return this;
        }

        public ConsoleConfig withTheme(Theme theme) {
            this.theme = theme;
            return this;
        }
        
        public ConsoleConfig withStderr(boolean stderr) {
            this.stderr = stderr;
            return this;
        }
        
        public ConsoleConfig withOutputFile(PrintStream outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public ConsoleConfig withQuiet(boolean quiet) {
            this.quiet = quiet;
            return this;
        }

        public ConsoleConfig withWidth(Integer width) {
            this.width = width;
            return this;
        }

        public ConsoleConfig withHeight(Integer height) {
            this.height = height;
            return this;
        }

        public ConsoleConfig withStyle(Object style) {
            this.style = style;
            return this;
        }

        public ConsoleConfig withNoColor(Boolean noColor) {
            this.noColor = noColor;
            return this;
        }

        public ConsoleConfig withTabSize(int tabSize) {
            this.tabSize = tabSize;
            return this;
        }

        public ConsoleConfig withRecord(boolean record) {
            this.record = record;
            return this;
        }

        public ConsoleConfig withMarkup(boolean markup) {
            this.markup = markup;
            return this;
        }

        public ConsoleConfig withEmoji(boolean emoji) {
            this.emoji = emoji;
            return this;
        }

        public ConsoleConfig withHighlight(boolean highlight) {
            this.highlight = highlight;
            return this;
        }

        public ConsoleConfig withSafeBox(boolean safeBox) {
            this.safeBox = safeBox;
            return this;
        }

        public ConsoleConfig withLegacyWindows(Boolean legacyWindows) {
            this.legacyWindows = legacyWindows;
            return this;
        }

        public ConsoleConfig withTerminalConfigurer(Consumer<TerminalBuilder> terminalConfigurer) {
            this.terminalConfigurer = terminalConfigurer;
            return this;
        }

        public Console build() {
            return new Console(
                    colorSystem, forceTerminal, forceJupyter, forceInteractive,
                    softWrap, theme, stderr, outputFile, quiet,
                    width, height, style, noColor, tabSize, record,
                    markup, emoji, highlight, logTimeFormat, highlighter,
                    safeBox, legacyWindows, terminalConfigurer
            );
        }
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Create a Console with default settings.
     */
    public Console() {
        this(null, null, null, null, false, null, false, null,
                false, null, null, null, false, DEFAULT_TAB_SIZE, false, true, true, true,
                null, null, true, null, null);
    }

    /**
     * Create a Console with a custom JLine TerminalBuilder configuration.
     *
     * @param terminalConfigurer consumer to customize the TerminalBuilder
     *                           (e.g., builder -&gt; builder.jna(true))
     */
    public Console(Consumer<TerminalBuilder> terminalConfigurer) {
        this(null, null, null, null, false, null, false, null,
                false, null, null, null, false, DEFAULT_TAB_SIZE, false, true, true, true,
                null, null, true, null, terminalConfigurer);
    }

    /**
     * Create a Console with a Config consumer for fluent configuration.
     *
     * <p>Usage: {@code Console.of(cfg -> cfg.forceTerminal(true).colorSystem("truecolor"))}</p>
     *
     * @param configurer consumer to configure the ConsoleConfig
     * @return a new Console with the configured options
     */
    public static Console of(Consumer<ConsoleConfig> configurer) {
        return ConsoleConfig.configure(configurer).build();
    }

    /**
     * Full constructor for Console.
     *
     * @param colorSystem     color system: "auto", "standard", "256", "truecolor", "windows", or null
     * @param forceTerminal   force terminal mode, or null to auto-detect
     * @param forceJupyter    force Jupyter mode, or null to auto-detect
     * @param forceInteractive force interactive mode, or null to auto-detect
     * @param softWrap        enable soft wrap (default false)
     * @param theme           theme to use, or null for default
     * @param stderr          write to stderr instead of stdout
     * @param outputFile      output print stream, or null for default
     * @param quiet           suppress all output
     * @param width           console width, or null to auto-detect from JLine
     * @param height          console height, or null to auto-detect from JLine
     * @param style           default style to apply to all output
     * @param noColor         disable colors, or null to auto-detect
     * @param tabSize         number of spaces per tab (default 8)
     * @param record          enable recording for HTML/SVG export
     * @param markup          enable markup parsing (default true)
     * @param emoji           enable emoji processing (default true)
     * @param highlight       enable auto-highlighting (default true)
     * @param logTimeFormat   time format for log (unused in this port)
     * @param highlighter     default highlighter (unused in this simplified port)
     * @param safeBox         restrict box options for legacy Windows (default true)
     * @param legacyWindows   enable legacy Windows mode, or null to auto-detect
     * @param terminalConfigurer consumer to customize JLine TerminalBuilder (e.g., set jna(true))
     */
    public Console(
            String colorSystem,
            Boolean forceTerminal,
            Boolean forceJupyter,
            Boolean forceInteractive,
            boolean softWrap,
            Theme theme,
            boolean stderr,
            PrintStream outputFile,
            boolean quiet,
            Integer width,
            Integer height,
            Object style,
            Boolean noColor,
            int tabSize,
            boolean record,
            boolean markup,
            boolean emoji,
            boolean highlight,
            Object logTimeFormat,
            Object highlighter,
            boolean safeBox,
            Boolean legacyWindows,
            Consumer<TerminalBuilder> terminalConfigurer
    ) {
        this.forceTerminal = forceTerminal;
        this.forceJupyter = forceJupyter != null && forceJupyter;
        this.softWrap = softWrap;
        this.theme = theme;
        this.stderr = stderr;
        this.outputFile = outputFile;
        this.quiet = quiet;
        this.width = width;
        this.height = height;
        this.style = style;
        this.tabSize = tabSize;
        this.record = record;
        this.markupEnabled = markup;
        this.emojiEnabled = emoji;
        this.highlightEnabled = highlight;
        this.safeBox = safeBox;

        // Create JLine Terminal (gracefully degrade if unavailable)
        this.terminal = createTerminal(terminalConfigurer);

        // Detect legacy Windows
        this.legacyWindows = legacyWindows != null ? legacyWindows : false;

        // Detect color system
        if (colorSystem == null || COLOR_SYSTEM_AUTO.equals(colorSystem)) {
            this.colorSystem = detectColorSystem();
        } else {
            this.colorSystem = COLOR_SYSTEMS.get(colorSystem);
        }

        // No color detection
        this.noColor = noColor != null ? noColor : false;

        // Interactive detection
        this.forceInteractive = forceInteractive;

        // Theme stack
        this.themeStack = new ThemeStack(theme != null ? theme : new Theme());

        // Log render
        this.logRender = new LogRender();

        // Highlighter
        if (highlighter instanceof ReprHighlighter) {
            this.highlighter = (ReprHighlighter) highlighter;
        } else {
            this.highlighter = new ReprHighlighter();
        }

        // Log time format
        this.logTimeFormat = logTimeFormat;
    }

    // =========================================================================
    // Properties
    // =========================================================================

    /**
     * Get the color system name string.
     */
    public String getColorSystem() {
        if (colorSystem != null) {
            return COLOR_SYSTEMS_NAMES.get(colorSystem);
        }
        return null;
    }

    /**
     * Get the ColorSystem enum value.
     */
    public ColorSystem getColorSystemEnum() {
        return colorSystem;
    }

    /**
     * Check if this console is writing to a terminal.
     * Uses JLine Terminal if available, falls back to System.console() check.
     */
    public boolean isTerminal() {
        if (forceTerminal != null) {
            return forceTerminal;
        }
        if (outputFile != null) {
            return false;
        }
        // Use JLine terminal if available
        if (terminal != null) {
            return !Terminal.TYPE_DUMB.equals(terminal.getType());
        }
        // Fallback: Check FORCE_COLOR / TTY_COMPATIBLE environment variables
        String forceColor = System.getenv("FORCE_COLOR");
        if (forceColor != null && !forceColor.isEmpty()) {
            return true;
        }
        String ttyCompat = System.getenv("TTY_COMPATIBLE");
        if (ttyCompat != null && !ttyCompat.isEmpty()) {
            return true;
        }
        // Fallback: System.console() exists and TERM is not "dumb"
        if (System.console() != null) {
            String term = System.getenv("TERM");
            if (term != null && term.toLowerCase().equals(TERM_TYPE_DUMB)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Get the JLine Terminal instance (may be null if terminal is unavailable).
     */
    public Terminal getTerminal() {
        return terminal;
    }

    /**
     * Check if this is a dumb terminal.
     */
    public boolean isDumbTerminal() {
        String term = System.getenv("TERM");
        if (term == null) {
            return false;
        }
        String lower = term.toLowerCase();
        return isTerminal() && (TERM_TYPE_DUMB.equals(lower) || TERM_TYPE_UNKNOWN.equals(lower));
    }

    /**
     * Check if interactive mode is enabled.
     */
    public boolean isInteractive() {
        if (forceInteractive != null) {
            return forceInteractive;
        }
        return isTerminal() && !isDumbTerminal();
    }

    /**
     * Check if we are in Jupyter mode.
     */
    public boolean isJupyter() {
        return forceJupyter;
    }

    /**
     * Check if no-color mode is enabled.
     */
    public boolean isNoColor() {
        return noColor;
    }

    /**
     * Get the encoding of the output.
     */
    public String getEncoding() {
        return DEFAULT_ENCODING;
    }

    /**
     * Get the size of the console.
     * Uses JLine Terminal if available for accurate size detection,
     * falls back to environment variables COLUMNS/LINES, then defaults.
     */
    public ConsoleDimensions getSize() {
        // If both dimensions are explicitly set, use them
        if (width != null && height != null) {
            return new ConsoleDimensions(width - (legacyWindows ? 1 : 0), height);
        }

        int detectedWidth = DEFAULT_WIDTH;
        int detectedHeight = DEFAULT_HEIGHT;

        // Try JLine Terminal first (most reliable)
        if (terminal != null) {
            try {
                org.jline.terminal.Size size = terminal.getSize();
                if (size != null && size.getColumns() > 0 && size.getRows() > 0) {
                    detectedWidth = size.getColumns();
                    detectedHeight = size.getRows();
                }
            } catch (Exception ignored) {
                // JLine failed, fall through
            }
        }

        // Fall back to environment variables
        if (detectedWidth == DEFAULT_WIDTH) {
            String columnsEnv = System.getenv("COLUMNS");
            if (isDigits(columnsEnv)) {
                detectedWidth = Integer.parseInt(columnsEnv);
            }
        }
        if (detectedHeight == DEFAULT_HEIGHT) {
            String linesEnv = System.getenv("LINES");
            if (isDigits(linesEnv)) {
                detectedHeight = Integer.parseInt(linesEnv);
            }
        }

        int finalWidth = this.width != null ? this.width : detectedWidth;
        int finalHeight = this.height != null ? this.height : detectedHeight;

        return new ConsoleDimensions(
                finalWidth - (legacyWindows && this.width == null ? 1 : 0),
                finalHeight
        );
    }

    /**
     * Set the console size.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Get the console width.
     */
    public int getWidth() {
        return getSize().width();
    }

    /**
     * Set the console width.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Get the console height.
     */
    public int getHeight() {
        return getSize().height();
    }

    /**
     * Set the console height.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Get default console options.
     */
    public ConsoleOptions getOptions() {
        ConsoleDimensions size = getSize();
        return new ConsoleOptions(
                size,
                legacyWindows,
                1,
                size.width(),
                isTerminal(),
                getEncoding(),
                size.height(),
                null, null,
                false, null, null, null
        );
    }

    /**
     * Get the tab size.
     */
    public int getTabSize() {
        return tabSize;
    }

    /**
     * Check if recording is enabled.
     */
    public boolean isRecording() {
        return record;
    }

    /**
     * Check if safe box mode is enabled.
     */
    public boolean isSafeBox() {
        return safeBox;
    }

    /**
     * Check if legacy Windows mode is enabled.
     */
    public boolean isLegacyWindows() {
        return legacyWindows;
    }

    /**
     * Check if the alt screen is active.
     */
    public boolean isAltScreen() {
        return isAltScreen;
    }

    // =========================================================================
    // Theme management
    // =========================================================================

    /**
     * Push a theme onto the theme stack.
     */
    public void pushTheme(Theme theme, boolean inherit) {
        themeStack.pushTheme(theme, inherit);
    }

    /**
     * Push a theme onto the theme stack (inheriting by default).
     */
    public void pushTheme(Theme theme) {
        themeStack.pushTheme(theme);
    }

    /**
     * Pop a theme from the stack.
     */
    public void popTheme() {
        themeStack.popTheme();
    }

    /**
     * Create a ThemeContext for use with try-with-resources.
     */
    public ThemeContext useTheme(Theme theme, boolean inherit) {
        return new ThemeContext(this, theme, inherit);
    }

    /**
     * Get a style by name or parse a style definition.
     *
     * @param name the style name or definition string
     * @return the resolved Style
     * @throws MissingStyle if the style cannot be found or parsed
     */
    public Style getStyle(Object name) {
        return getStyle(name, null);
    }

    /**
     * Get a style by name or parse a style definition, with a fallback default.
     */
    public Style getStyle(Object name, Object defaultStyle) {
        if (name instanceof Style) {
            return (Style) name;
        }

        String nameStr = name != null ? name.toString() : "";
        if (nameStr.isEmpty()) {
            return Style.nullStyle();
        }

        // Try to get from theme stack first
        Style style = themeStack.get(nameStr);
        if (style != null) {
            return style.getLink() != null ? style.copy() : style;
        }

        // Try to parse as a style definition
        try {
            style = Style.parse(nameStr);
            return style;
        } catch (StyleSyntaxError error) {
            // Not a valid style definition, use default or return null style
            if (defaultStyle != null) {
                return getStyle(defaultStyle);
            }
            return Style.nullStyle();
        }
    }

    // =========================================================================
    // Render hooks
    // =========================================================================

    /**
     * Push a render hook.
     */
    public void pushRenderHook(RenderHook hook) {
        lock.lock();
        try {
            renderHooks.add(hook);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Pop the last render hook.
     */
    public void popRenderHook() {
        lock.lock();
        try {
            if (!renderHooks.isEmpty()) {
                renderHooks.remove(renderHooks.size() - 1);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set Live instance. Used by Live context manager.
     *
     * @param live the Live instance using this Console
     * @return true if this is the topmost live on the stack
     */
    public boolean setLive(Live live) {
        lock.lock();
        try {
            liveStack.add(live);
            return liveStack.size() == 1;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clear the Live instance. Used by Live context manager.
     */
    public void clearLive() {
        lock.lock();
        try {
            if (!liveStack.isEmpty()) {
                liveStack.remove(liveStack.size() - 1);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the current (topmost) Live instance.
     *
     * @return the current Live, or null if no Live is active
     */
    public Live getCurrentLive() {
        lock.lock();
        try {
            if (!liveStack.isEmpty()) {
                return liveStack.get(liveStack.size() - 1);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    // =========================================================================
    // Buffer management
    // =========================================================================

    /**
     * Enter a buffer context (increment buffer index).
     */
    public void enterBuffer() {
        bufferIndex++;
    }

    /**
     * Exit a buffer context (decrement buffer index and flush if needed).
     */
    public void exitBuffer() {
        bufferIndex--;
        checkBuffer();
    }

    /**
     * Begin capturing console output.
     */
    public void beginCapture() {
        enterBuffer();
    }

    /**
     * End capture mode and return the captured string.
     */
    public String endCapture() {
        String result = renderBuffer(new ArrayList<>(buffer));
        buffer.clear();
        exitBuffer();
        return result;
    }

    /**
     * Console itself can be used as a buffer context (AutoCloseable).
     */
    public Console enter() {
        enterBuffer();
        return this;
    }

    /**
     * Exit the buffer context.
     */
    public void exit() {
        exitBuffer();
    }

    // =========================================================================
    // Core rendering
    // =========================================================================

    /**
     * Render an object into an iterable of Segments.
     *
     * @param renderable the object to render
     * @param options    console options, or null for default
     * @return an iterable of Segment instances
     */
    public Iterable<Segment> render(Object renderable, ConsoleOptions options) {
        ConsoleOptions opts = options != null ? options : getOptions();
        if (opts.getMaxWidth() < 1) {
            return Collections.emptyList();
        }

        // Apply rich_cast
        Object cast = Protocol.richCast(renderable);

        Iterable<?> renderIterable;

        if (cast instanceof RichRenderable) {
            renderIterable = ((RichRenderable) cast).richConsole(this, opts);
        } else if (cast instanceof String) {
            Text textRenderable = renderStr((String) cast,
                    opts.getHighlight(), opts.getMarkup());
            renderIterable = textRenderable.richConsole(this, opts);
        } else {
            throw new NotRenderableError(
                    "Unable to render " + cast + "; "
                            + "A str, Segment or object with richConsole method is required");
        }

        // Flatten the render result
        List<Segment> result = new ArrayList<>();
        ConsoleOptions resetOpts = opts.resetHeight();
        flattenRenderResult(renderIterable, resetOpts, result);
        return result;
    }

    /**
     * Render an object using default options.
     */
    public Iterable<Segment> render(Object renderable) {
        return render(renderable, null);
    }

    /**
     * Render objects into a list of lines (lists of segments).
     *
     * @param renderable the object to render
     * @param options    console options, or null for default
     * @param style      optional style to apply
     * @param pad        whether to pad lines shorter than render width
     * @param newLines   whether to include newline segments at end of lines
     * @return a list of lines, where each line is a list of Segments
     */
    public List<List<Segment>> renderLines(Object renderable, ConsoleOptions options,
                                            Style style, boolean pad, boolean newLines) {
        lock.lock();
        try {
            ConsoleOptions renderOptions = options != null ? options : getOptions();
            Iterable<Segment> rendered = render(renderable, renderOptions);
            if (style != null) {
                rendered = Segment.applyStyle(rendered, style, null);
            }

            Integer renderHeight = renderOptions.getHeight();
            if (renderHeight != null) {
                renderHeight = Math.max(0, renderHeight);
            }

            int maxLines = renderHeight != null ? renderHeight : Integer.MAX_VALUE;
            List<List<Segment>> lines = new ArrayList<>();
            Iterator<List<Segment>> it = Segment.splitAndCropLines(
                    rendered, renderOptions.getMaxWidth(), style, pad, newLines
            ).iterator();

            int count = 0;
            while (it.hasNext() && count < maxLines) {
                lines.add(it.next());
                count++;
            }

            if (renderHeight != null) {
                int extraLines = renderHeight - lines.size();
                if (extraLines > 0) {
                    List<Segment> padLine;
                    if (newLines) {
                        padLine = new ArrayList<>();
                        padLine.add(new Segment(spaces(renderOptions.getMaxWidth()), style));
                        padLine.add(Segment.line());
                    } else {
                        padLine = new ArrayList<>();
                        padLine.add(new Segment(spaces(renderOptions.getMaxWidth()), style));
                    }
                    for (int i = 0; i < extraLines; i++) {
                        lines.add(new ArrayList<>(padLine));
                    }
                }
            }

            return lines;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Render lines with default options.
     */
    public List<List<Segment>> renderLines(Object renderable) {
        return renderLines(renderable, null, null, true, false);
    }

    /**
     * Convert a string to a Text instance.
     *
     * @param text       the text to render
     * @param style      base style (string or Style)
     * @param justify    justification method
     * @param overflow   overflow method
     * @param emoji      enable emoji, or null for console default
     * @param markup     enable markup, or null for console default
     * @param highlight  enable highlighting, or null for console default
     * @return a Text instance
     */
    public Text renderStr(String text, Object style, String justify, String overflow,
                           Boolean emoji, Boolean markup, Boolean highlight) {
        boolean emojiEnabled = emoji != null ? emoji : this.emojiEnabled;
        boolean markupEnabled = markup != null ? markup : this.markupEnabled;
        boolean highlightEnabled = highlight != null ? highlight : this.highlightEnabled;

        Text richText;
        if (markupEnabled) {
            List<Markup.StyledSpan> spans = Markup.render(text,
                    style instanceof String ? (String) style : "", emojiEnabled);

            // Build plain text from spans (stripping markup tags)
            StringBuilder plainBuilder = new StringBuilder();
            for (Markup.StyledSpan span : spans) {
                plainBuilder.append(span.text());
            }
            String plainText = plainBuilder.toString();

            richText = new Text(plainText);
            // Apply styles from spans using correct offsets into plain text
            int offset = 0;
            for (Markup.StyledSpan span : spans) {
                if (!span.style().isEmpty()) {
                    Style parsedStyle = Style.parse(span.style());
                    richText.stylize(parsedStyle, offset, offset + span.text().length());
                }
                offset += span.text().length();
            }

        } else {
            richText = new Text(text, style);
        }
        if (justify != null) {
            richText.setJustify(justify);
        }
        if (overflow != null) {
            richText.setOverflow(overflow);
        }

        // Set end="" so that trailing newlines are controlled by print()'s end parameter,
        // not by the Text object itself. This matches Python rich's _collect_renderables
        // which sets end through the join text, not on individual render_str results.
        richText.setEnd("");

        if (highlightEnabled && this.highlighter != null) {
            richText = this.highlighter.apply(richText);
        }

        return richText;
    }

    /**
     * Simplified renderStr.
     */
    public Text renderStr(String text, Boolean highlight, Boolean markup) {
        return renderStr(text, null, null, null, null, markup, highlight);
    }

    /**
     * RenderStr with just text.
     */
    public Text renderStr(String text) {
        return renderStr(text, null, null);
    }

    // =========================================================================
    // Print methods
    // =========================================================================

    /**
     * Print objects to the console.
     *
     * @param objects       the objects to print
     * @param sep           separator between objects (default " ")
     * @param end           string to write at the end (default "\n")
     * @param style         optional style to apply
     * @param justify       justification method
     * @param overflow      overflow method
     * @param noWrap        disable wrapping
     * @param emoji         enable emoji
     * @param markup        enable markup
     * @param highlight     enable highlighting
     * @param width         override width
     * @param height        override height
     * @param crop          crop output to console width
     * @param softWrap      enable soft wrap
     * @param newLineStart  insert new line at start if output is multiline
     */
    public void print(Object[] objects, String sep, String end, Object style,
                       String justify, String overflow, Boolean noWrap,
                       Boolean emoji, Boolean markup, Boolean highlight,
                       Integer width, Integer height, boolean crop,
                       Boolean softWrap, boolean newLineStart) {

        if (objects == null || objects.length == 0) {
            objects = new Object[]{new NewLine()};
        }

        boolean actualSoftWrap = softWrap != null ? softWrap : this.softWrap;
        if (actualSoftWrap) {
            if (noWrap == null) {
                noWrap = true;
            }
            if (overflow == null) {
                overflow = OVERFLOW_IGNORE;
            }
            crop = false;
        }

        List<RenderHook> hooks;
        lock.lock();
        try {
            hooks = new ArrayList<>(renderHooks);
        } finally {
            lock.unlock();
        }

        enterBuffer();
        try {
            List<Object> renderables = collectRenderables(
                    objects, sep, end, justify, emoji, markup, highlight);

            // end is now handled inside collectRenderables (baked into joined Text),
            // matching Python rich's approach where end is only applied to Text objects,
            // not to RichRenderables which already include trailing newlines.

            for (RenderHook hook : hooks) {
                List<?> processed = hook.processRenderables(renderables);
                renderables = new ArrayList<>(processed);
            }

            ConsoleOptions renderOptions = getOptions().update(
                    width != null ? Math.min(width, getWidth()) : null,
                    null, null,
                    justify, overflow, noWrap,
                    highlight, markup, height
            );

            List<Segment> newSegments = new ArrayList<>();

            if (style == null) {
                for (Object renderable : renderables) {
                    Iterable<Segment> rendered = render(renderable, renderOptions);
                    for (Segment seg : rendered) {
                        newSegments.add(seg);
                    }
                }
            } else {
                Style renderStyle = getStyle(style);
                Segment newLine = Segment.line();
                for (Object renderable : renderables) {
                    Iterable<Segment> rendered = render(renderable, renderOptions);
                    for (List<Segment> line : Segment.splitLines(rendered)) {
                        Iterable<Segment> styled = Segment.applyStyle(line, renderStyle, null);
                        for (Segment seg : styled) {
                            newSegments.add(seg);
                        }
                        newSegments.add(newLine);
                    }
                }
            }

            if (newLineStart) {
                // Check if output has more than one line
                int lineCount = 0;
                for (Segment seg : newSegments) {
                    if ("\n".equals(seg.getText())) {
                        lineCount++;
                        if (lineCount > 1) break;
                    }
                }
                if (lineCount > 1) {
                    newSegments.add(0, Segment.line());
                }
            }

            if (crop) {
                for (List<Segment> line : Segment.splitAndCropLines(
                        newSegments, getWidth(), null, false, true)) {
                    buffer.addAll(line);
                }
            } else {
                buffer.addAll(newSegments);
            }

            // 'end' is now included in renderables, so no separate handling needed
        } finally {
            exitBuffer();
        }
    }

    /**
     * Print with default settings (no trailing newline).
     * Follows Java convention: print() does not add a newline.
     */
    public void print(Object... objects) {
        print(objects, " ", "", null, null, null, null,
                null, null, null, null, null, true, null, false);
    }

    /**
     * Print a single object with a style (no trailing newline).
     */
    public void print(Object object, Object style) {
        print(new Object[]{object}, " ", "", style, null, null, null,
                null, null, null, null, null, true, null, false);
    }

    /**
     * Print with trailing newline.
     * Follows Java convention: println() adds a newline at the end.
     */
    public void println(Object... objects) {
        print(objects, " ", "\n", null, null, null, null,
                null, null, null, null, null, true, null, false);
    }

    /**
     * Print a single object with a style, followed by newline.
     */
    public void println(Object object, Object style) {
        print(new Object[]{object}, " ", "\n", style, null, null, null,
                null, null, null, null, null, true, null, false);
    }

    /**
     * Print an empty line (just a newline).
     */
    public void println() {
        print(new Object[]{}, " ", "\n", null, null, null, null,
                null, null, null, null, null, true, null, false);
    }

    /**
     * Print raw text without markup parsing.
     * Useful for printing ANSI strings that shouldn't be parsed as markup.
     * Outputs directly to the underlying PrintStream, bypassing Text rendering.
     *
     * @param text the text to print (may contain ANSI escape sequences)
     */
    public void printRaw(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        PrintStream out = outputFile != null ? outputFile : (stderr ? System.err : System.out);
        out.print(text);
        if (!text.endsWith("\n")) {
            out.println();
        }
        out.flush();
    }

    /**
     * Print raw text without markup parsing and without trailing newline.
     * Outputs directly to the underlying PrintStream, bypassing Text rendering.
     *
     * @param text the text to print (may contain ANSI escape sequences)
     */
    public void printRawNoNewline(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        PrintStream out = outputFile != null ? outputFile : (stderr ? System.err : System.out);
        out.print(text);
        out.flush();
    }

    /**
     * Print with custom separator and end.
     */
    public void print(Object[] objects, String sep, String end) {
        print(objects, sep, end, null, null, null, null,
                null, null, null, null, null, true, null, false);
    }

    /**
     * Low-level output to the terminal. Unlike print(), this won't pretty print,
     * wrap text, or apply markup, but will optionally apply highlighting and style.
     * Matches Python rich's console.out().
     *
     * @param objects    objects to output
     * @param sep        separator between objects
     * @param end        string after all objects
     * @param style      optional style to apply
     * @param highlight  enable auto-highlighting
     */
    public void out(Object[] objects, String sep, String end, Object style, Boolean highlight) {
        if (objects == null || objects.length == 0) {
            objects = new Object[]{new NewLine()};
        }
        String rawOutput = String.join(sep != null ? sep : " ",
                java.util.Arrays.stream(objects).map(String::valueOf).collect(java.util.stream.Collectors.toList()));
        print(new Object[]{rawOutput}, " ", end, style, null, "ignore",
                true, false, false, highlight, null, null, false, null, false);
    }

    /**
     * Low-level output with default settings.
     */
    public void out(String text) {
        out(new Object[]{text}, " ", "\n", null, null);
    }

    // =========================================================================
    // Convenience methods
    // =========================================================================

    /**
     * Draw a horizontal rule with an optional centered title.
     * Convenience method that creates a Rule and prints it.
     *
     * @param title the title text (may include markup)
     */
    public void rule(Object title) {
        print(new Rule(title));
    }

    /**
     * Draw a horizontal rule with customizable options.
     * Convenience method matching Python rich's console.rule().
     *
     * @param title      the title text (may include markup)
     * @param characters character(s) to form the rule line (default "─")
     * @param style      style for the rule line (e.g., "bold red", Style.parse("blue"))
     * @param align      title alignment: "left", "center", or "right"
     */
    public void rule(Object title, String characters, Object style, String align) {
        print(new Rule(title, characters, style, align));
    }

    /**
     * Draw a horizontal rule with Config-style configuration.
     * <pre>{@code console.rule("Title", cfg -> cfg.style("red").align("left"))}</pre>
     */
    public void rule(Object title, Consumer<Rule.Config> configurer) {
        print(new Rule(title, configurer));
    }

    /**
     * Log rich content to the terminal with timestamp and caller info.
     * Convenience method matching Python rich's console.log().
     *
     * <p>Output includes a time column, message, and caller path (file:line).
     * Objects are processed through collectRenderables like print().</p>
     *
     * @param objects objects to log
     */
    public void log(Object... objects) {
        log(objects, " ", "\n", null, null, null, null, null);
    }

    /**
     * Log rich content with full options.
     * Matching Python rich's console.log() signature.
     *
     * @param objects   objects to log
     * @param sep       separator between objects
     * @param end       string after all objects
     * @param style     optional style to apply
     * @param justify   justification method
     * @param emoji     enable emoji processing
     * @param markup    enable markup processing
     * @param highlight enable auto-highlighting
     */
    public void log(Object[] objects, String sep, String end, Object style,
                    String justify, Boolean emoji, Boolean markup, Boolean highlight) {
        if (objects == null || objects.length == 0) {
            objects = new Object[]{new NewLine()};
        }

        // Don't include end in the message renderables — the log table
        // already includes a trailing newline from being a RichRenderable
        List<Object> renderables = collectRenderables(
                objects, sep, "", justify, emoji, markup, highlight);

        if (style != null) {
            Style renderStyle = getStyle(style);
            List<Object> styled = new ArrayList<>();
            for (Object r : renderables) {
                styled.add(new Styled(r, renderStyle));
            }
            renderables = styled;
        }

        // Get caller info (skip log() frame and callerFrameInfo frame)
        String[] callerInfo = callerFrameInfo(3);
        String filename = callerInfo[0];
        Integer lineNo = callerInfo[1] != null ? Integer.parseInt(callerInfo[1]) : null;
        String path = filename;
        int lastSep = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (lastSep >= 0) {
            path = filename.substring(lastSep + 1);
        }
        String linkPath = filename.startsWith("<") ? null : filename;

        // Build log table and print it (matches Python rich's approach)
        Table logTable = logRender.render(this, renderables,
                java.time.LocalDateTime.now(), null, path, lineNo, linkPath);
        print(logTable);
    }

    /**
     * Get caller frame information for log output.
     * Returns [filename, lineNumber].
     */
    private String[] callerFrameInfo(int skipFrames) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // stack[0] = getStackTrace, stack[1] = callerFrameInfo,
        // stack[2] = log, stack[3+] = actual caller
        int index = skipFrames + 1; // +1 for callerFrameInfo itself
        if (index < stack.length) {
            StackTraceElement caller = stack[index];
            return new String[]{caller.getFileName(), String.valueOf(caller.getLineNumber())};
        }
        return new String[]{"<unknown>", "0"};
    }

    /**
     * Write new line(s).
     */
    public void line(int count) {
        print(new NewLine(count));
    }

    /**
     * Write a single new line.
     */
    public void line() {
        line(1);
    }

    /**
     * Clear the screen.
     *
     * @param home also move cursor to home position
     */
    public void clear(boolean home) {
        if (home) {
            control(Control.clear(), Control.home());
        } else {
            control(Control.clear());
        }
    }

    /**
     * Clear the screen and move cursor home.
     */
    public void clear() {
        clear(true);
    }

    /**
     * Play a bell sound.
     */
    public void bell() {
        control(Control.bell());
    }

    /**
     * Print a rich-formatted traceback for the given Throwable.
     * Matches Python rich's console.print_exception().
     *
     * @param throwable  the throwable to display
     * @param width      width for the traceback (default 100)
     * @param extraLines number of extra source lines around the error line
     * @param theme      syntax highlighting theme name
     * @param wordWrap   enable word wrapping
     * @param showLocals show local variables in each frame
     * @param maxFrames  maximum number of frames to display
     */
    public void printException(Throwable throwable, int width, int extraLines,
                               String theme, boolean wordWrap, boolean showLocals,
                               int maxFrames) {
        Traceback.Trace trace = Traceback.fromThrowable(throwable, showLocals);
        Traceback traceback = Traceback.of(trace, cfg -> cfg
                .width(width).codeWidth(88).extraLines(extraLines)
                .theme(theme).wordWrap(wordWrap).showLocals(showLocals).maxFrames(maxFrames));
        print(traceback);
    }

    /**
     * Print a rich-formatted traceback for the given Throwable with default options.
     */
    public void printException(Throwable throwable) {
        printException(throwable, 100, 3, "monokai", true, false, 100);
    }

    /**
     * Pretty print JSON data with syntax highlighting.
     * Convenience method matching Python rich's console.print_json().
     *
     * @param json the JSON string to render
     */
    public void printJson(String json) {
        printJson(json, 2, true);
    }

    /**
     * Pretty print JSON data with syntax highlighting.
     *
     * @param json       the JSON string to render
     * @param indent     number of spaces for indentation
     * @param highlight  whether to enable syntax highlighting
     */
    public void printJson(String json, int indent, boolean highlight) {
        print(new JSON(json, indent, highlight));
    }

    // =========================================================================
    // Input methods
    // =========================================================================

    /**
     * Read a line of input from the user.
     * Renders the prompt as rich text, then waits for input via JLine.
     *
     * @param prompt   the prompt text (may include markup)
     * @param password if true, echo characters as '*'
     * @return the user's input string
     */
    public String input(Object prompt, boolean password) {
        // Render the prompt to plain text for JLine
        String promptStr;
        if (prompt instanceof Text) {
            promptStr = ((Text) prompt).getPlain();
        } else if (prompt instanceof String) {
            // Render markup to get plain text
            Text rendered = renderStr((String) prompt, null, null, null, true, true, false);
            promptStr = rendered.getPlain();
        } else {
            promptStr = prompt.toString();
        }

        try {
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            if (password) {
                return reader.readLine(promptStr, '\0');
            } else {
                return reader.readLine(promptStr);
            }
        } catch (Exception e) {
            // Fallback: use System.console() or BufferedReader
            System.out.print(promptStr);
            try {
                return new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
            } catch (java.io.IOException ex) {
                return "";
            }
        }
    }

    /**
     * Read a line of input from the user.
     *
     * @param prompt the prompt text (may include markup)
     * @return the user's input string
     */
    public String input(Object prompt) {
        return input(prompt, false);
    }

    /**
     * Context manager for alternate screen mode.
     * Returns an AutoCloseable that enables alt screen on creation and restores on close.
     * Usage: {@code try (var ignored = console.screen()) { ... }}
     *
     * @param hideCursor whether to hide the cursor while in alt screen
     * @param style      optional style for the screen
     * @return an AutoCloseable that restores the screen on close
     */
    public AutoCloseable screen(boolean hideCursor, Object style) {
        setAltScreen(true);
        if (hideCursor) {
            showCursor(false);
        }
        return () -> {
            if (hideCursor) {
                showCursor(true);
            }
            setAltScreen(false);
        };
    }

    /**
     * Context manager for alternate screen mode with cursor hidden.
     */
    public AutoCloseable screen() {
        return screen(true, null);
    }

    /**
     * Show or hide the cursor.
     *
     * @return true if the control code was written
     */
    public boolean showCursor(boolean show) {
        if (isTerminal()) {
            control(Control.showCursor(show));
            return true;
        }
        return false;
    }

    /**
     * Show the cursor.
     */
    public boolean showCursor() {
        return showCursor(true);
    }

    /**
     * Enable or disable alternate screen.
     *
     * @return true if the control codes were written
     */
    public boolean setAltScreen(boolean enable) {
        boolean changed = false;
        if (isTerminal() && !legacyWindows) {
            control(Control.altScreen(enable));
            changed = true;
            isAltScreen = enable;
        }
        return changed;
    }

    /**
     * Set the window title.
     *
     * @return true if the control code was written
     */
    public boolean setWindowTitle(String title) {
        if (isTerminal()) {
            control(Control.title(title));
            return true;
        }
        return false;
    }

    /**
     * Insert non-printing control codes.
     */
    public void control(Control... controls) {
        if (!isDumbTerminal()) {
            enterBuffer();
            try {
                for (Control ctrl : controls) {
                    Iterable<?> segments = ctrl.richConsole(this, null);
                    for (Object seg : segments) {
                        if (seg instanceof Segment) {
                            buffer.add((Segment) seg);
                        }
                    }
                }
            } finally {
                exitBuffer();
            }
        }
    }

    /**
     * Measure a renderable.
     */
    public Measurement measure(Object renderable, ConsoleOptions options) {
        ConsoleOptions opts = options != null ? options : getOptions();
        return Measurement.get(this, opts, renderable);
    }

    /**
     * Measure a renderable with default options.
     */
    public Measurement measure(Object renderable) {
        return measure(renderable, null);
    }

    // =========================================================================
    // Screen update
    // =========================================================================

    /**
     * Update the screen at a given region.
     */
    public void updateScreen(Object renderable, Region region, ConsoleOptions options) {
        if (!isAltScreen) {
            throw new NoAltScreen("Alt screen must be enabled to call updateScreen");
        }
        ConsoleOptions renderOptions = options != null ? options : getOptions();
        int x, y;
        if (region == null) {
            x = y = 0;
            renderOptions = renderOptions.updateDimensions(
                    renderOptions.getMaxWidth(),
                    renderOptions.getHeight() != null ? renderOptions.getHeight() : getHeight()
            );
        } else {
            x = region.x();
            y = region.y();
            renderOptions = renderOptions.updateDimensions(region.width(), region.height());
        }

        List<List<Segment>> lines = renderLines(renderable, renderOptions, null, true, false);
        updateScreenLines(lines, x, y);
    }

    /**
     * Update screen lines at a given offset.
     */
    public void updateScreenLines(List<List<Segment>> lines, int x, int y) {
        if (!isAltScreen) {
            throw new NoAltScreen("Alt screen must be enabled to call updateScreenLines");
        }
        ScreenUpdate screenUpdate = new ScreenUpdate(lines, x, y);
        Iterable<Segment> segments = render(screenUpdate);
        buffer.addAll(new ArrayList<>());
        for (Segment seg : segments) {
            buffer.add(seg);
        }
        checkBuffer();
    }

    // =========================================================================
    // Capture
    // =========================================================================

    /**
     * Create a Capture context.
     */
    public Capture capture() {
        return new Capture(this);
    }

    /**
     * Create a Status context with default spinner ("dots").
     *
     * @param status the status text (supports Rich markup)
     * @return a Status instance that can be used with try-with-resources
     */
    public Status status(Object status) {
        Status s = new Status(status, this);
        s.start();
        return s;
    }

    /**
     * Create a Status context with custom spinner.
     *
     * @param status           the status text
     * @param spinner          spinner animation name (e.g. "dots", "line", "earth")
     * @param spinnerStyle     style for the spinner animation
     * @param speed            animation speed multiplier
     * @param refreshPerSecond refresh rate per second
     * @return a Status instance
     */
    public Status status(
            Object status, String spinner, Object spinnerStyle,
            double speed, double refreshPerSecond) {
        Status s = new Status(
                status, this, spinner, spinnerStyle, speed, refreshPerSecond);
        s.start();
        return s;
    }

    /**
     * Create a Progress instance with this Console pre-set.
     * Usage: {@code try (Progress p = console.progress(cfg -> cfg.transientMode = true)) { ... }}
     *
     * @param configurer consumer to configure the Progress
     * @return a Progress instance (not yet started — call {@link Progress#start()})
     */
    public Progress progress(Consumer<Progress.Config> configurer) {
        return Progress.of(cfg -> {
            cfg.console = this;
            configurer.accept(cfg);
        });
    }

    /**
     * Create a Progress instance with this Console pre-set and default settings.
     *
     * @return a Progress instance (not yet started — call {@link Progress#start()})
     */
    public Progress progress() {
        return Progress.of(cfg -> cfg.console = this);
    }

    // =========================================================================
    // Export methods
    // =========================================================================

    /**
     * Export recorded content as plain text.
     *
     * @param clear  clear the record buffer after exporting
     * @param styles include ANSI style codes
     * @return the exported text
     */
    public String exportText(boolean clear, boolean styles) {
        if (!record) {
            throw new IllegalStateException("To export console contents set record=True in the constructor");
        }
        recordBufferLock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            if (styles) {
                for (Segment segment : recordBuffer) {
                    Style segStyle = segment.getStyle();
                    if (segStyle != null) {
                        sb.append(segStyle.render(segment.getText(), colorSystem, legacyWindows));
                    } else {
                        sb.append(segment.getText());
                    }
                }
            } else {
                for (Segment segment : recordBuffer) {
                    if (!segment.isControl()) {
                        sb.append(segment.getText());
                    }
                }
            }
            if (clear) {
                recordBuffer.clear();
            }
            return sb.toString();
        } finally {
            recordBufferLock.unlock();
        }
    }

    /**
     * Export text with default settings.
     */
    public String exportText() {
        return exportText(true, false);
    }

    /**
     * Save exported text to a file path.
     */
    public void saveText(String path, boolean clear, boolean styles) {
        String text = exportText(clear, styles);
        try {
            java.io.File file = new java.io.File(path);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save text to " + path, e);
        }
    }

    // =========================================================================
    // Internal methods
    // =========================================================================

    /**
     * Create a JLine Terminal instance.
     * Returns null if terminal creation fails (e.g., in constrained environments
     * like Gradle execution, IDEs, or CI systems).
     *
     * @param terminalConfigurer optional consumer to customize the TerminalBuilder
     * @return a Terminal instance, or null if creation failed
     */
    private Terminal createTerminal(Consumer<TerminalBuilder> terminalConfigurer) {
        try {
            TerminalBuilder builder = TerminalBuilder.builder()
                    .system(true)
                    .dumb(true); // allow dumb terminal fallback
            if (terminalConfigurer != null) {
                terminalConfigurer.accept(builder);
            }
            Terminal t = builder.build();
            return t;
        } catch (Exception e) {
            // JLine Terminal creation failed — common in IDE/Gradle/CI environments.
            // This is non-fatal; Console will fall back to env vars and defaults.
            return null;
        }
    }

    /**
     * Detect the color system from environment.
     */
    private ColorSystem detectColorSystem() {
        if (forceJupyter) {
            return ColorSystem.TRUECOLOR;
        }
        if (!isTerminal() || isDumbTerminal()) {
            return null;
        }

        // Check COLORTERM for truecolor support
        String colorTerm = System.getenv("COLORTERM");
        if (colorTerm != null) {
            String lower = colorTerm.trim().toLowerCase();
            if (COLORTERM_TRUECOLOR.equals(lower) || COLORTERM_24BIT.equals(lower)) {
                return ColorSystem.TRUECOLOR;
            }
        }

        String term = System.getenv("TERM");
        if (term != null) {
            String lower = term.trim().toLowerCase();
            int hyphenIdx = lower.lastIndexOf('-');
            if (hyphenIdx >= 0) {
                String colors = lower.substring(hyphenIdx + 1);
                switch (colors) {
                    case "kitty", "256color" -> {
                        return ColorSystem.EIGHT_BIT;
                    }
                    case "16color" -> {
                        return ColorSystem.STANDARD;
                    }
                }
            }
        }

        // On Windows, modern terminals (Windows Terminal, ConEmu, etc.) support truecolor
        // When forceTerminal is true, assume at least 256-color support
        if (forceTerminal != null && forceTerminal) {
            return ColorSystem.TRUECOLOR;
        }

        return ColorSystem.STANDARD;
    }

    /**
     * Collect renderables from print arguments.
     * Inserts separator Text between consecutive renderables, matching
     * Python's _collect_renderables behavior.
     */
    private List<Object> collectRenderables(Object[] objects, String sep, String end,
                                             String justify, Boolean emoji,
                                             Boolean markup, Boolean highlight) {
        List<Object> renderables = new ArrayList<>();
        String effectiveSep = sep != null ? sep : " ";
        List<Text> textBuffer = new ArrayList<>();

        for (Object obj : objects) {
            Object cast = Protocol.richCast(obj);

            if (cast instanceof String) {
                Text rendered = renderStr((String) cast, null, justify, null,
                        emoji, markup, highlight);
                textBuffer.add(rendered);
            } else if (cast instanceof Text) {
                textBuffer.add((Text) cast);
            } else if (cast instanceof RichRenderable) {
                // Flush any accumulated text before adding a non-Text renderable
                flushTextBuffer(renderables, textBuffer, effectiveSep, end, justify);
                renderables.add(cast);
            } else {
                // Convert unknown objects to string → Text
                Text rendered = renderStr(String.valueOf(cast), null, justify, null,
                        emoji, markup, highlight);
                textBuffer.add(rendered);
            }
        }

        // Flush any remaining accumulated text
        flushTextBuffer(renderables, textBuffer, effectiveSep, end, justify);

        // Apply console style if set
        if (style != null) {
            Style consoleStyle = getStyle(style);
            List<Object> styledRenderables = new ArrayList<>();
            for (Object r : renderables) {
                styledRenderables.add(new Styled(r, consoleStyle));
            }
            renderables = styledRenderables;
        }

        return renderables;
    }

    /**
     * Flush accumulated Text objects into a single joined Text with end set.
     * This matches Python rich's _collect_renderables approach where
     * end is only applied to Text objects, not to RichRenderables
     * (which already include their own trailing newlines).
     */
    private void flushTextBuffer(List<Object> renderables, List<Text> textBuffer,
                                  String sep, String end, String justify) {
        if (!textBuffer.isEmpty()) {
            Text sepText = new Text(sep);
            sepText.setEnd(end != null ? end : "");
            sepText.setJustify(justify);
            Text joined = Text.join(sepText, textBuffer);
            renderables.add(joined);
            textBuffer.clear();
        }
    }

    /**
     * Flatten a render result (which may contain nested renderables) into segments.
     */
    private void flattenRenderResult(Iterable<?> renderResult, ConsoleOptions options,
                                      List<Segment> out) {
        for (Object item : renderResult) {
            if (item instanceof Segment) {
                out.add((Segment) item);
            } else {
                // Recursively render
                Object cast = Protocol.richCast(item);
                if (cast instanceof RichRenderable) {
                    Iterable<?> nested = ((RichRenderable) cast).richConsole(this, options);
                    flattenRenderResult(nested, options, out);
                } else if (cast instanceof String) {
                    Text textRenderable = renderStr((String) cast,
                            options.getHighlight(), options.getMarkup());
                    Iterable<?> nested = textRenderable.richConsole(this, options);
                    flattenRenderResult(nested, options, out);
                }
            }
        }
    }

    /**
     * Check if the buffer should be rendered, and render it if so.
     */
    private void checkBuffer() {
        if (quiet) {
            buffer.clear();
            return;
        }
        writeBuffer();
    }

    /**
     * Write the buffer to the output file.
     */
    private void writeBuffer() {
        lock.lock();
        try {
            if (record && bufferIndex == 0) {
                recordBufferLock.lock();
                try {
                    recordBuffer.addAll(buffer);
                } finally {
                    recordBufferLock.unlock();
                }
            }

            if (bufferIndex == 0) {
                String text = renderBuffer(new ArrayList<>(buffer));
                if (!text.isEmpty()) {
                    PrintStream out = outputFile;
                    if (out == null) {
                        out = stderr ? System.err : System.out;
                    }
                    // Write in chunks to avoid issues with very long strings
                    if (text.length() <= MAX_WRITE) {
                        out.print(text);
                    } else {
                        List<String> batch = new ArrayList<>();
                        int size = 0;
                        int start = 0;
                        for (int i = 0; i < text.length(); i++) {
                            if (text.charAt(i) == '\n') {
                                String line = text.substring(start, i + 1);
                                if (size + line.length() > MAX_WRITE && !batch.isEmpty()) {
                                    StringBuilder sb = new StringBuilder();
                                    for (String s : batch) sb.append(s);
                                    out.print(sb);
                                    batch.clear();
                                    size = 0;
                                }
                                batch.add(line);
                                size += line.length();
                                start = i + 1;
                            }
                        }
                        // Remaining text after last newline
                        if (start < text.length()) {
                            batch.add(text.substring(start));
                        }
                        if (!batch.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (String s : batch) sb.append(s);
                            out.print(sb);
                        }
                    }
                    out.flush();
                }
                buffer.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Render a list of segments into a string with ANSI codes.
     */
    private String renderBuffer(List<Segment> buf) {
        StringBuilder output = new StringBuilder();
        Iterable<Segment> segments = buf;

        if (noColor && colorSystem != null) {
            segments = Segment.removeColor(segments);
        }

        for (Segment segment : segments) {
            String text = segment.getText();
            Style segStyle = segment.getStyle();
            boolean isControl = segment.isControl();

            if (segStyle != null) {
                output.append(segStyle.render(text, colorSystem, legacyWindows));
            } else if (!(isControl && !isTerminal())) {
                output.append(text);
            }
        }

        return output.toString();
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Generate a string of spaces.
     */
    private static String spaces(int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Check if a string consists entirely of digits.
     */
    private static boolean isDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "<console width=" + getWidth() + " " + colorSystem + ">";
    }
}
