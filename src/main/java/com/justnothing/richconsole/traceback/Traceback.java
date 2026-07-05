package com.justnothing.richconsole.traceback;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.box.Box;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.pretty.Pretty;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.syntax.Syntax;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Span;
import com.justnothing.richconsole.text.Text;

/**
 * A renderable for Python-style tracebacks from Java Throwables.
 * Ported from rich/traceback.py Traceback class.
 *
 * <p>Renders Java exceptions with styled panels, optional source code
 * extraction, and cause/suppressed chain display.</p>
 */
public class Traceback implements RichRenderable {

    // =========================================================================
    // Inner data classes
    // =========================================================================

    /** Represents one stack frame from a StackTraceElement. */
    public static class Frame {
        private final String filename;
        private final String className;
        private final String methodName;
        private final int lineno;
        private final boolean nativeMethod;
        private final Map<String, Object> locals;

        public Frame(String filename, String className, String methodName,
                     int lineno, boolean nativeMethod, Map<String, Object> locals) {
            this.filename = filename;
            this.className = className;
            this.methodName = methodName;
            this.lineno = lineno;
            this.nativeMethod = nativeMethod;
            this.locals = locals;
        }

        public String getFilename() { return filename; }
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public int getLineno() { return lineno; }
        public boolean isNativeMethod() { return nativeMethod; }
        public Map<String, Object> getLocals() { return locals; }
    }

    /** Represents one exception's stack trace. */
    public static class Stack {
        private final String excType;
        private final String excValue;
        private final boolean isCause;
        private final List<Frame> frames;
        private final List<Stack> suppressed;

        public Stack(String excType, String excValue, boolean isCause,
                     List<Frame> frames, List<Stack> suppressed) {
            this.excType = excType;
            this.excValue = excValue;
            this.isCause = isCause;
            this.frames = frames != null ? frames : Collections.emptyList();
            this.suppressed = suppressed != null ? suppressed : Collections.emptyList();
        }

        public String getExcType() { return excType; }
        public String getExcValue() { return excValue; }
        public boolean isCause() { return isCause; }
        public List<Frame> getFrames() { return frames; }
        public List<Stack> getSuppressed() { return suppressed; }
    }

    /** The complete traceback with cause chain. */
    public static class Trace {
        private final List<Stack> stacks;

        public Trace(List<Stack> stacks) {
            this.stacks = stacks != null ? stacks : Collections.emptyList();
        }

        public List<Stack> getStacks() { return stacks; }
    }

    // =========================================================================
    // Instance fields
    // =========================================================================

    private final Trace trace;
    private final int width;
    private final int codeWidth;
    private final int extraLines;
    private final String theme;
    private final boolean wordWrap;
    private final boolean showLocals;
    private final int maxFrames;
    private final List<String> suppress;

    // =========================================================================
    // Config — fluent configuration for Traceback construction
    // =========================================================================

    /**
     * Fluent configuration object for Traceback construction.
     * Usage: {@code Traceback.of(trace, cfg -> cfg.width(120).theme("github-dark").showLocals(true))}
     */
    public static class Config {
        public int width = 100;
        public int codeWidth = 88;
        public int extraLines = 3;
        public String theme = "monokai";
        public boolean wordWrap = false;
        public boolean showLocals = false;
        public int maxFrames = 10;
        public List<String> suppress;

        public Config width(int width) { this.width = width; return this; }
        public Config codeWidth(int codeWidth) { this.codeWidth = codeWidth; return this; }
        public Config extraLines(int extraLines) { this.extraLines = extraLines; return this; }
        public Config theme(String theme) { this.theme = theme; return this; }
        public Config wordWrap(boolean wordWrap) { this.wordWrap = wordWrap; return this; }
        public Config showLocals(boolean showLocals) { this.showLocals = showLocals; return this; }
        public Config maxFrames(int maxFrames) { this.maxFrames = maxFrames; return this; }
        public Config suppress(List<String> suppress) { this.suppress = suppress; return this; }
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    public Traceback(Trace trace, int width, int codeWidth, int extraLines,
                     String theme, boolean wordWrap, boolean showLocals,
                     int maxFrames, List<String> suppress) {
        this.trace = trace;
        this.width = width;
        this.codeWidth = codeWidth;
        this.extraLines = extraLines;
        this.theme = theme;
        this.wordWrap = wordWrap;
        this.showLocals = showLocals;
        this.maxFrames = maxFrames;
        this.suppress = suppress != null ? suppress : Collections.emptyList();
    }

    private Traceback(Trace trace, Config cfg) {
        this.trace = trace;
        this.width = cfg.width;
        this.codeWidth = cfg.codeWidth;
        this.extraLines = cfg.extraLines;
        this.theme = cfg.theme;
        this.wordWrap = cfg.wordWrap;
        this.showLocals = cfg.showLocals;
        this.maxFrames = cfg.maxFrames;
        this.suppress = cfg.suppress != null ? cfg.suppress : Collections.emptyList();
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Create a Traceback with a Config consumer for fluent configuration.
     * <pre>{@code
     * Traceback.of(trace, cfg -> cfg.width(120).theme("github-dark").showLocals(true))
     * }</pre>
     *
     * @param trace      the Trace object (required)
     * @param configurer a consumer that configures the Traceback options
     * @return a new Traceback instance
     */
    public static Traceback of(Trace trace, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Traceback(trace, cfg);
    }

    /**
     * Extract traceback info from a Java Throwable.
     *
     * @param throwable   the throwable to extract from
     * @param showLocals  if true, use reflection to get declared fields of the exception object
     * @return a Trace object
     */
    public static Trace fromThrowable(Throwable throwable, boolean showLocals) {
        return fromThrowable(throwable, showLocals, Collections.emptyList());
    }

    /**
     * Extract traceback info from a Java Throwable with a suppress list.
     *
     * @param throwable   the throwable to extract from
     * @param showLocals  if true, use reflection to get declared fields of the exception object
     * @param suppress    list of path prefixes to suppress
     * @return a Trace object
     */
    public static Trace fromThrowable(Throwable throwable, boolean showLocals, List<String> suppress) {
        List<Stack> stacks = new ArrayList<>();
        Set<Throwable> visited = new HashSet<>();
        collectStacks(throwable, false, showLocals, visited, stacks, suppress);
        return new Trace(stacks);
    }

    /**
     * Convenience factory that creates Traceback from Throwable with default options.
     */
    public static Traceback fromThrowable(Throwable throwable) {
        Trace trace = fromThrowable(throwable, false);
        return new Traceback(trace, 100, 88, 3, "monokai",
                true, false, 100, Collections.emptyList());
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public Iterable<Segment> richConsole(Console console, ConsoleOptions options) {
        List<Segment> result = new ArrayList<>();

        if (trace == null || trace.getStacks().isEmpty()) {
            return result;
        }

        List<Stack> stacks = trace.getStacks();
        Style borderStyle = console.getStyle("traceback.border");

        // Render main exception first, then causes below (正序遍历)
        // stacks[0] is topmost exception, stacks[1...] are causes
        Stack mainStack = stacks.get(0);
        List<Stack> causes = stacks.size() > 1 ? stacks.subList(1, stacks.size()) : Collections.emptyList();

        // Build main panel content
        List<Object> mainContent = new ArrayList<>();

        // Render frames for main exception
        renderFrames(mainContent, console, options, mainStack);

        // Exception type and message
        Text excText = new Text();
        excText.setEnd("\n");
        excText.append(mainStack.getExcType(), "bold red");
        if (mainStack.getExcValue() != null && !mainStack.getExcValue().isEmpty()) {
            excText.append(": ");
            excText.append(mainStack.getExcValue(), "red");
        }
        mainContent.add(excText);

        // Suppressed exceptions (if any)
        if (!mainStack.getSuppressed().isEmpty()) {
            renderSuppressed(mainContent, console, options, mainStack.getSuppressed());
        }

        // Render first cause as nested panel (which will recursively include further causes)
        if (!causes.isEmpty()) {
            Panel causePanel = renderCauseChain(console, options, causes, 0);
            mainContent.add(causePanel);
        }

        // Main panel title
        Text title = new Text("Traceback ");
        title.append("(most recent call last)", "dim");

        // Create main panel
        Panel mainPanel = new Panel(
            new Group(mainContent),
            title, null,
            Box.ROUNDED,
            true,
            null,
            borderStyle,
            width > 0 ? width : null,
            null,
            new int[]{0, 1, 0, 1}
        );

        return toSegmentList(mainPanel.richConsole(console, options));
    }

    // =========================================================================
    // Frame rendering
    // =========================================================================

    private void renderFrames(List<Object> content, Console console, ConsoleOptions options, Stack stack) {
        List<Frame> frames = stack.getFrames();
        int frameCount = frames.size();
        boolean truncated = false;
        if (maxFrames > 0 && frameCount > maxFrames) {
            frames = frames.subList(frameCount - maxFrames, frameCount);
            truncated = true;
        }

        if (truncated) {
            Text ellipsis = new Text("  ...(" + (frameCount - maxFrames) + " frame(s) hidden)...", "dim");
            ellipsis.setEnd("\n");
            content.add(ellipsis);
        }

        for (Frame frame : frames) {
            content.add(buildLocationText(frame));

            // Skip source code rendering for suppressed frames
            if (!shouldSuppress(frame.getFilename())) {
                // Try to extract and render source code
                List<String> sourceLines = extractSource(frame);
                if (sourceLines != null && !sourceLines.isEmpty()) {
                    String code = String.join("\n", sourceLines);
                    int startLine = Math.max(1, frame.getLineno() - extraLines);
                    Set<Integer> highlightLines = new HashSet<>();
                    highlightLines.add(frame.getLineno());
                    Syntax syntax = Syntax.of(code, cfg -> cfg
                            .lexerName("java").lineNumbers(true).startLine(startLine)
                            .highlightLines(highlightLines)
                            .themeName(theme).codeWidth(codeWidth).wordWrap(wordWrap));
                    content.add(syntax);
                }
            }

            // Render locals if available
            if (showLocals && frame.getLocals() != null && !frame.getLocals().isEmpty()) {
                content.add(renderScope(frame.getLocals()));
            }
        }
    }

    // =========================================================================
    // Cause chain rendering (recursive)
    // =========================================================================

    private Panel renderCauseChain(Console console, ConsoleOptions options,
                                   List<Stack> causes, int index) {
        Style borderStyle = console.getStyle("traceback.border");
        Stack cause = causes.get(index);

        List<Object> content = new ArrayList<>();

        // Render frames for this cause
        renderFrames(content, console, options, cause);

        // Exception type and message
        Text excText = new Text();
        excText.setEnd("\n");
        excText.append(cause.getExcType(), "bold red");
        if (cause.getExcValue() != null && !cause.getExcValue().isEmpty()) {
            excText.append(": ");
            excText.append(cause.getExcValue(), "red");
        }
        content.add(excText);

        // Suppressed exceptions (if any)
        if (!cause.getSuppressed().isEmpty()) {
            renderSuppressed(content, console, options, cause.getSuppressed());
        }

        // Recursively render the next cause (nested inside this panel)
        if (index + 1 < causes.size()) {
            Panel nestedCause = renderCauseChain(console, options, causes, index + 1);
            content.add(nestedCause);
        }

        // Simple title: "Cause" (no number needed — nesting shows hierarchy)
        Text title = new Text("Cause ");
        title.append("(most recent call last)", "dim");

        return new Panel(
            new Group(content),
            title, null,
            Box.ROUNDED,
            true,
            null,
            borderStyle,
            width > 0 ? width - 2 * (index + 1) : null, // progressively narrower for deeper nesting
            null,
            new int[]{0, 1, 0, 1}
        );
    }

    // =========================================================================
    // Suppressed rendering
    // =========================================================================

    private void renderSuppressed(List<Object> panelContent, Console console,
                                  ConsoleOptions options, List<Stack> suppressedList) {
        // Single "Suppressed" label
        Text label = new Text();
        label.setEnd("\n");
        label.append("Suppressed:", "bold yellow");
        panelContent.add(label);

        for (Stack suppressed : suppressedList) {
            // Indented frame location
            for (Frame frame : suppressed.getFrames()) {
                Text location = buildLocationText(frame);
                // Prepend indent
                Text indented = new Text();
                indented.setEnd("\n");
                indented.append("  ");
                indented.append(location.getPlain(), location.getStyle());
                // Copy style spans from location
                for (Span span : location.getSpans()) {
                    indented.stylize(span.style(), span.start() + 2, span.end() + 2);
                }
                panelContent.add(indented);
            }

            // Exception type and message
            Text excText = new Text();
            excText.setEnd("\n");
            excText.append("  ");
            excText.append(suppressed.getExcType(), "bold yellow");
            if (suppressed.getExcValue() != null && !suppressed.getExcValue().isEmpty()) {
                excText.append(": ");
                excText.append(suppressed.getExcValue(), "yellow");
            }
            panelContent.add(excText);

            // Recursively render nested suppressed
            if (!suppressed.getSuppressed().isEmpty()) {
                renderSuppressed(panelContent, console, options, suppressed.getSuppressed());
            }
        }
    }

    // =========================================================================
    // Frame rendering
    // =========================================================================

    private Text buildLocationText(Frame frame) {
        Text location = new Text();
        location.setEnd("\n");

        String filename = frame.getFilename();
        if (filename == null || filename.isEmpty()) {
            filename = frame.getClassName();
        }

        // VSCode-style colors: filename=cyan, class=green, method=yellow
        location.append(filename, "cyan");
        if (frame.getLineno() > 0) {
            location.append(":" + frame.getLineno(), "traceback.offset");
        }

        location.append(" in ", "dim");
        location.append(frame.getClassName(), "green");
        location.append(".", null);
        location.append(frame.getMethodName(), "yellow");

        if (frame.isNativeMethod()) {
            location.append(" ", "dim");
            location.append("[native]", "dim");
        }

        return location;
    }

    // =========================================================================
    // Source extraction
    // =========================================================================

    private static List<String> extractSource(Frame frame) {
        if (frame.isNativeMethod() || frame.getLineno() <= 0) {
            return null;
        }

        String className = frame.getClassName();
        String resourcePath = className.replace('.', '/') + ".java";

        try (InputStream is = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }

            List<String> allLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
            }

            int targetLine = frame.getLineno();
            int startLine = Math.max(1, targetLine - 3);
            int endLine = Math.min(allLines.size(), targetLine + 3);

            if (startLine > allLines.size()) {
                return null;
            }

            return allLines.subList(startLine - 1, endLine);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // Scope rendering
    // =========================================================================

    private Panel renderScope(Map<String, Object> locals) {
        Table grid = Table.grid(1);
        grid.addColumn("key");
        grid.addColumn("equals");
        grid.addColumn("value");

        for (Map.Entry<String, Object> entry : locals.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Choose key style
            String keyStyle;
            if (key.startsWith("_") || key.startsWith("$")) {
                keyStyle = "scope.key.special";
            } else {
                keyStyle = "scope.key";
            }

            Text keyText = Text.styled(key, keyStyle);
            Text equalsText = Text.styled("=", "scope.equals");
            Object valueRenderable;
            if (value == null) {
                valueRenderable = Text.styled("null", "repr.none");
            } else {
                valueRenderable = new Pretty(value);
            }

            grid.addRow(keyText, equalsText, valueRenderable);
        }

        return new Panel(grid, null, null, Box.SIMPLE, false,
                null, "scope.border", null, null, new int[]{0, 1, 0, 1});
    }

    // =========================================================================
    // Throwable extraction helpers
    // =========================================================================

    private static void collectStacks(Throwable throwable, boolean isCause,
                                      boolean showLocals, Set<Throwable> visited,
                                      List<Stack> stacks, List<String> suppress) {
        if (throwable == null || visited.contains(throwable)) {
            return;
        }
        visited.add(throwable);

        String excType = throwable.getClass().getSimpleName();

        // Extract frames from stack trace
        List<Frame> frames = new ArrayList<>();
        StackTraceElement[] elements = throwable.getStackTrace();
        for (StackTraceElement element : elements) {
            Map<String, Object> locals = null;
            if (showLocals) {
                locals = extractLocals(throwable);
            }
            frames.add(new Frame(
                element.getFileName(),
                element.getClassName(),
                element.getMethodName(),
                element.getLineNumber(),
                element.isNativeMethod(),
                locals
            ));
        }

        // Extract suppressed exceptions
        List<Stack> suppressedStacks = new ArrayList<>();
        for (Throwable suppressed : throwable.getSuppressed()) {
            collectStacks(suppressed, false, showLocals, visited, suppressedStacks, suppress);
        }

        String excValue = safeStr(throwable.getMessage());
        stacks.add(new Stack(excType, excValue, isCause, frames, suppressedStacks));

        // Walk the cause chain
        Throwable cause = throwable.getCause();
        if (cause != null) {
            collectStacks(cause, true, showLocals, visited, stacks, suppress);
        }
    }

    private boolean shouldSuppress(String filename) {
        if (suppress == null || suppress.isEmpty() || filename == null || filename.isEmpty()) {
            return false;
        }
        for (String path : suppress) {
            if (path != null && filename.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> extractLocals(Throwable throwable) {
        Map<String, Object> locals = new LinkedHashMap<>();
        try {
            Class<?> cls = throwable.getClass();
            for (Field field : cls.getDeclaredFields()) {
                field.setAccessible(true);
                String key = field.getName();
                Object value = field.get(throwable);
                locals.put(key, value);
            }
        } catch (Exception e) {
            // Reflection failed, return what we have
        }
        return locals;
    }

    private static String safeStr(Object obj) {
        if (obj == null) return null;
        try {
            return obj.toString();
        } catch (Exception e) {
            return "<error getting string representation>";
        }
    }

    // =========================================================================
    // Group helper (inline simple group of renderables)
    // =========================================================================

    private static class Group implements RichRenderable {
        private final List<?> items;

        Group(List<?> items) {
            this.items = items;
        }

        @Override
        public Iterable<?> richConsole(Console console, ConsoleOptions options) {
            return items;
        }
    }

    private static List<Segment> toSegmentList(Iterable<?> iterable) {
        List<Segment> list = new ArrayList<>();
        if (iterable != null) {
            for (Object item : iterable) {
                if (item instanceof Segment s) {
                    list.add(s);
                }
            }
        }
        return list;
    }
}
