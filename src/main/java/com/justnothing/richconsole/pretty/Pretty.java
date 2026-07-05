package com.justnothing.richconsole.pretty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Pretty-print representations of Java objects.
 * Ported from rich/pretty.py Pretty class.
 *
 * <p>Width-aware rendering: when the inline representation of a collection
 * would exceed the available width, it is automatically expanded to multi-line
 * rendering, matching Python rich's behavior.</p>
 */
public class Pretty implements RichRenderable {

    private static final int MAX_LENGTH = 80;
    private static final int MAX_COLLECTION_ITEMS = 20;

    private final Object object;
    private final boolean highlight;
    private final boolean indentGuides;
    private final int indent;
    private final Set<Integer> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    // Width constraint for inline vs multi-line decision (set from options)
    private int maxWidth = 80;

    // =========================================================================
    // Config
    // =========================================================================

    /**
     * Fluent configuration object for Pretty construction.
     * Usage: {@code Pretty.of(myObject, cfg -> cfg.highlight(false).indentGuides(true))}
     */
    public static class Config {
        public boolean highlight = true;
        public boolean indentGuides = false;

        public Config highlight(boolean highlight) { this.highlight = highlight; return this; }
        public Config indentGuides(boolean indentGuides) { this.indentGuides = indentGuides; return this; }
    }

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Create a Pretty with fluent configuration.
     * <pre>{@code
     * Pretty.of(myObject, cfg -> cfg.highlight(false).indentGuides(true))
     * }</pre>
     *
     * @param object      the object to pretty-print (required)
     * @param configurer  a consumer that configures the Pretty options
     * @return a new Pretty instance
     */
    public static Pretty of(Object object, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Pretty(object, cfg);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    private Pretty(Object object, Config cfg) {
        this(object, cfg.highlight, cfg.indentGuides);
    }

    public Pretty(Object object) {
        this(object, true);
    }

    public Pretty(Object object, boolean highlight) {
        this(object, highlight, true);
    }

    public Pretty(Object object, boolean highlight, boolean indentGuides) {
        this.object = object;
        this.highlight = highlight;
        this.indentGuides = indentGuides;
        this.indent = 4;
    }

    public Object getObject() { return object; }
    public boolean isHighlight() { return highlight; }
    public boolean isIndentGuides() { return indentGuides; }
    public int getIndent() { return indent; }

    /**
     * Returns true for types that have an expandable representation
     * (Maps, Lists, and arrays).
     */
    public static boolean isExpandable(Object obj) {
        return obj instanceof Map || obj instanceof List || obj != null && obj.getClass().isArray();
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        this.maxWidth = options.getMaxWidth();
        List<Segment> segments = new ArrayList<>();
        renderObject(segments, object, console, 0, 0);
        segments.add(Segment.line());
        return segments;
    }

    // =========================================================================
    // Internal rendering
    // =========================================================================

    /**
     * Render an object.
     * @param lineOffset characters already used on the current line (e.g., key prefix)
     */
    private void renderObject(List<Segment> segments, Object obj, Console console, int depth, int lineOffset) {
        if (obj instanceof Map) {
            int identity = System.identityHashCode(obj);
            if (!visited.add(identity)) {
                segments.add(new Segment("...", Style.nullStyle()));
                return;
            }
            renderMap(segments, (Map<?, ?>) obj, console, depth, lineOffset);
            visited.remove(identity);
        } else if (obj instanceof List) {
            int identity = System.identityHashCode(obj);
            if (!visited.add(identity)) {
                segments.add(new Segment("...", Style.nullStyle()));
                return;
            }
            renderList(segments, (List<?>) obj, console, depth, lineOffset);
            visited.remove(identity);
        } else if (obj != null && obj.getClass().isArray()) {
            int identity = System.identityHashCode(obj);
            if (!visited.add(identity)) {
                segments.add(new Segment("...", Style.nullStyle()));
                return;
            }
            renderArray(segments, obj, console, depth, lineOffset);
            visited.remove(identity);
        } else {
            renderScalar(segments, obj, console);
        }
    }

    private void renderMap(List<Segment> segments, Map<?, ?> map, Console console, int depth, int lineOffset) {
        Style braceStyle = resolveStyle(console, "repr.brace");

        segments.add(new Segment("{", braceStyle));

        if (map.isEmpty()) {
            segments.add(new Segment("}", braceStyle));
            return;
        }

        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            segments.add(Segment.line());
            int indentWidth = addIndent(segments, depth + 1, console);

            if (count >= MAX_COLLECTION_ITEMS) {
                segments.add(new Segment("...", resolveStyle(console, "repr.comma")));
                break;
            }

            // render key
            int keyWidth = estimateKeyWidth(entry.getKey());
            renderKey(segments, entry.getKey(), console);
            segments.add(new Segment(": "));

            // render value — pass key prefix as lineOffset so value can decide inline vs multi-line
            int valueLineOffset = indentWidth + keyWidth + 2; // 2 for ": "
            renderObject(segments, entry.getValue(), console, depth + 1, valueLineOffset);

            if (count < map.size() - 1 && count < MAX_COLLECTION_ITEMS - 1) {
                segments.add(new Segment(",", resolveStyle(console, "repr.comma")));
            }
            count++;
        }

        segments.add(Segment.line());
        addIndent(segments, depth, console);
        segments.add(new Segment("}", braceStyle));
    }

    private void renderList(List<Segment> segments, List<?> list, Console console, int depth, int lineOffset) {
        Style braceStyle = resolveStyle(console, "repr.brace");

        boolean onlyScalars = isSimpleList(list);
        boolean shouldInline = onlyScalars && fitsInline(list, depth, lineOffset);

        segments.add(new Segment("[", braceStyle));
        if (shouldInline) {
            // Inline rendering for simple lists that fit the width
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    segments.add(new Segment(", ", resolveStyle(console, "repr.comma")));
                }
                if (i >= MAX_COLLECTION_ITEMS) {
                    segments.add(new Segment("...", resolveStyle(console, "repr.comma")));
                    break;
                }
                renderScalar(segments, list.get(i), console);
            }
        } else {
            // Multi-line rendering for complex or wide lists

            for (int i = 0; i < list.size(); i++) {
                segments.add(Segment.line());
                addIndent(segments, depth + 1, console);

                if (i >= MAX_COLLECTION_ITEMS) {
                    segments.add(new Segment("...", resolveStyle(console, "repr.comma")));
                    break;
                }

                renderObject(segments, list.get(i), console, depth + 1, 0);

                if (i < list.size() - 1 && i < MAX_COLLECTION_ITEMS - 1) {
                    segments.add(new Segment(",", resolveStyle(console, "repr.comma")));
                }
            }

            segments.add(Segment.line());
            addIndent(segments, depth, console);
        }
        segments.add(new Segment("]", braceStyle));
    }

    private void renderArray(List<Segment> segments, Object array, Console console, int depth, int lineOffset) {
        Style braceStyle = resolveStyle(console, "repr.brace");

        if (array instanceof Object[] objArray) {
            boolean onlyScalars = isSimpleArray(objArray);
            boolean shouldInline = onlyScalars && fitsArrayInline(objArray, depth, lineOffset);

            segments.add(new Segment("[", braceStyle));
            if (shouldInline) {
                // Inline rendering for simple arrays that fit the width
                for (int i = 0; i < objArray.length; i++) {
                    if (i > 0) {
                        segments.add(new Segment(", ", resolveStyle(console, "repr.comma")));
                    }
                    if (i >= MAX_COLLECTION_ITEMS) {
                        segments.add(new Segment("...", resolveStyle(console, "repr.comma")));
                        break;
                    }
                    renderScalar(segments, objArray[i], console);
                }
            } else {
                // Multi-line rendering for complex or wide arrays

                for (int i = 0; i < objArray.length; i++) {
                    segments.add(Segment.line());
                    addIndent(segments, depth + 1, console);

                    if (i >= MAX_COLLECTION_ITEMS) {
                        segments.add(new Segment("...", resolveStyle(console, "repr.comma")));
                        break;
                    }

                    renderObject(segments, objArray[i], console, depth + 1, 0);

                    if (i < objArray.length - 1 && i < MAX_COLLECTION_ITEMS - 1) {
                        segments.add(new Segment(",", resolveStyle(console, "repr.comma")));
                    }
                }

                segments.add(Segment.line());
                addIndent(segments, depth, console);
            }
            segments.add(new Segment("]", braceStyle));
        } else {
            // Primitive arrays: inline if fits, multi-line otherwise
            int estimatedWidth = estimatePrimitiveArrayWidth(array);
            if (estimatedWidth + lineOffset + depth * indent <= maxWidth) {
                segments.add(new Segment("[", braceStyle));
                renderPrimitiveArray(segments, array, console);
                segments.add(new Segment("]", braceStyle));
            } else {
                segments.add(new Segment("[", braceStyle));
                int len = java.lang.reflect.Array.getLength(array);
                for (int i = 0; i < len; i++) {
                    segments.add(Segment.line());
                    addIndent(segments, depth + 1, console);
                    if (i >= MAX_COLLECTION_ITEMS) {
                        segments.add(new Segment("...", resolveStyle(console, "repr.comma")));
                        break;
                    }
                    Object value = java.lang.reflect.Array.get(array, i);
                    Style numberStyle = resolveStyle(console, "repr.number");
                    segments.add(new Segment(String.valueOf(value), numberStyle));
                    if (i < len - 1 && i < MAX_COLLECTION_ITEMS - 1) {
                        segments.add(new Segment(",", resolveStyle(console, "repr.comma")));
                    }
                }
                segments.add(Segment.line());
                addIndent(segments, depth, console);
                segments.add(new Segment("]", braceStyle));
            }
        }
    }

    private void renderPrimitiveArray(List<Segment> segments, Object array, Console console) {
        Style commaStyle = resolveStyle(console, "repr.comma");
        Style numberStyle = resolveStyle(console, "repr.number");

        int len = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                segments.add(new Segment(", ", commaStyle));
            }
            if (i >= MAX_COLLECTION_ITEMS) {
                segments.add(new Segment("...", commaStyle));
                break;
            }
            Object value = java.lang.reflect.Array.get(array, i);
            segments.add(new Segment(String.valueOf(value), numberStyle));
        }
    }

    private void renderScalar(List<Segment> segments, Object obj, Console console) {
        if (obj == null) {
            Style noneStyle = resolveStyle(console, "repr.none");
            segments.add(new Segment("null", noneStyle));
            return;
        }

        String text = obj.toString();
        if (obj instanceof String) {
            Style strStyle = resolveStyle(console, "repr.str");
            segments.add(new Segment("\"" + truncate(text) + "\"", strStyle));
        } else if (obj instanceof Number) {
            Style numberStyle = resolveStyle(console, "repr.number");
            segments.add(new Segment(truncate(text), numberStyle));
        } else if (obj instanceof Boolean) {
            Style boolStyle = resolveStyle(console, "repr.bool");
            segments.add(new Segment(text, boolStyle));
        } else if (obj instanceof Character) {
            Style strStyle = resolveStyle(console, "repr.str");
            segments.add(new Segment("'" + text + "'", strStyle));
        } else {
            segments.add(new Segment(truncate(text)));
        }
    }

    private void renderKey(List<Segment> segments, Object key, Console console) {
        Style keyStyle = resolveStyle(console, "repr.str");
        if (key instanceof String) {
            segments.add(new Segment("\"" + truncate(key.toString()) + "\"", keyStyle));
        } else if (key instanceof Number) {
            segments.add(new Segment(truncate(key.toString()), keyStyle));
        } else {
            segments.add(new Segment(truncate(key != null ? key.toString() : "null")));
        }
    }

    // =========================================================================
    // Indentation helpers
    // =========================================================================

    /**
     * Add indentation segments and return the total width of the indentation.
     */
    // TODO: use console to get theme style "repr.indent" instead of hardcoded "dim"
    private int addIndent(List<Segment> segments, int depth, Console console) {
        Style guideStyle = Style.parse("dim");
        int spacesPerLevel = indent;
        int guideWidth = 1;  // width of the │ character

        for (int d = 0; d < depth; d++) {
            if (indentGuides) {
                segments.add(new Segment("│", guideStyle));
                int remainingSpaces = spacesPerLevel - guideWidth;
                if (remainingSpaces > 0) {
                    segments.add(new Segment(spaces(remainingSpaces), Style.nullStyle()));
                }
            } else {
                segments.add(new Segment(spaces(spacesPerLevel), Style.nullStyle()));
            }
        }
        return depth * spacesPerLevel;
    }

    private static String spaces(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Returns true if a list contains only scalar values (no nested Maps/Lists/arrays).
     */
    private boolean isSimpleList(List<?> list) {
        for (Object item : list) {
            if (item instanceof Map || item instanceof List
                    || (item != null && item.getClass().isArray())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if an Object[] contains only scalar values (no nested Maps/Lists/arrays).
     */
    private boolean isSimpleArray(Object[] array) {
        for (Object item : array) {
            if (item instanceof Map || item instanceof List
                    || (item != null && item.getClass().isArray())) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Width estimation — decide inline vs multi-line
    // =========================================================================

    /**
     * Check if a list's inline representation fits within the available width.
     * @param depth current nesting depth
     * @param lineOffset characters already used on the current line (e.g., key prefix)
     */
    private boolean fitsInline(List<?> list, int depth, int lineOffset) {
        int indentWidth = depth * indent;
        int available = maxWidth - indentWidth - lineOffset;
        int width = estimateInlineListWidth(list);
        return width <= available;
    }

    /**
     * Check if an array's inline representation fits within the available width.
     * @param depth current nesting depth
     * @param lineOffset characters already used on the current line
     */
    private boolean fitsArrayInline(Object[] array, int depth, int lineOffset) {
        int indentWidth = depth * indent;
        int available = maxWidth - indentWidth - lineOffset;
        int width = estimateInlineArrayWidth(array);
        return width <= available;
    }

    /**
     * Estimate the cell width of a list rendered inline.
     * Format: [item1, item2, item3]
     */
    private int estimateInlineListWidth(List<?> list) {
        int width = 2; // brackets []
        int count = Math.min(list.size(), MAX_COLLECTION_ITEMS);
        for (int i = 0; i < count; i++) {
            if (i > 0) width += 2; // ", "
            width += estimateScalarWidth(list.get(i));
        }
        if (list.size() > MAX_COLLECTION_ITEMS) {
            width += 3; // "..."
        }
        return width;
    }

    /**
     * Estimate the cell width of an array rendered inline.
     */
    private int estimateInlineArrayWidth(Object[] array) {
        int width = 2; // brackets []
        int count = Math.min(array.length, MAX_COLLECTION_ITEMS);
        for (int i = 0; i < count; i++) {
            if (i > 0) width += 2; // ", "
            width += estimateScalarWidth(array[i]);
        }
        if (array.length > MAX_COLLECTION_ITEMS) {
            width += 3; // "..."
        }
        return width;
    }

    /**
     * Estimate the cell width of a primitive array rendered inline.
     */
    private int estimatePrimitiveArrayWidth(Object array) {
        int width = 2; // brackets []
        int len = java.lang.reflect.Array.getLength(array);
        int count = Math.min(len, MAX_COLLECTION_ITEMS);
        for (int i = 0; i < count; i++) {
            if (i > 0) width += 2; // ", "
            Object value = java.lang.reflect.Array.get(array, i);
            width += String.valueOf(value).length();
        }
        if (len > MAX_COLLECTION_ITEMS) {
            width += 3; // "..."
        }
        return width;
    }

    /**
     * Estimate the cell width of a scalar value.
     */
    private int estimateScalarWidth(Object obj) {
        if (obj == null) return 4; // "null"
        if (obj instanceof String) return obj.toString().length() + 2; // quotes
        if (obj instanceof Character) return 3; // 'x'
        return truncate(obj.toString()).length();
    }

    /**
     * Estimate the cell width of a key.
     */
    private int estimateKeyWidth(Object key) {
        if (key instanceof String) return key.toString().length() + 2; // quotes
        if (key == null) return 4;
        return truncate(key.toString()).length();
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private Style resolveStyle(Console console, String styleName) {
        if (!highlight) {
            return Style.nullStyle();
        }
        try {
            return console.getStyle(styleName);
        } catch (Exception e) {
            return Style.nullStyle();
        }
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH - 3) + "...";
    }
}
