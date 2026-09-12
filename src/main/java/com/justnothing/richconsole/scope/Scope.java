package com.justnothing.richconsole.scope;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.pretty.Pretty;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;

/**
 * Render a set of named variables, similar to Python rich's {@code render_scope()}.
 *
 * <p>Python's {@code render_scope(locals())} relies on the caller passing a
 * mapping of names to values; Java has no {@code locals()} equivalent, so the
 * caller supplies the mapping explicitly ({@link #of(Map)}), or lets the
 * renderer reflect the instance fields of an object ({@link #ofObject(Object)}).</p>
 *
 * <p>Rendering follows Python rich's scope.py: a grid table with the key column
 * right-justified, the value rendered by {@link Pretty}, wrapped in a
 * {@code Panel.fit} with the "scope.border" style.</p>
 */
public class Scope implements RichRenderable {

    /**
     * Fluent configuration.
     * <pre>{@code
     * Scope.of(scope, cfg -> cfg.title("locals").sortKeys(false).indentGuides(true))
     * }</pre>
     */
    public static class Config {
        public Object title;
        public boolean sortKeys = true;
        public boolean indentGuides = false;

        public Config title(Object title) { this.title = title; return this; }
        public Config sortKeys(boolean sortKeys) { this.sortKeys = sortKeys; return this; }
        public Config indentGuides(boolean indentGuides) { this.indentGuides = indentGuides; return this; }
    }

    private final Map<String, Object> scope;
    private final Object title;
    private final boolean sortKeys;
    private final boolean indentGuides;

    private Scope(Map<String, Object> scope, Config cfg) {
        this.scope = scope;
        this.title = cfg.title;
        this.sortKeys = cfg.sortKeys;
        this.indentGuides = cfg.indentGuides;
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Create a scope renderable from an explicit name-to-value mapping.
     */
    public static Scope of(Map<String, Object> scope) {
        return of(scope, cfg -> {});
    }

    /**
     * Create a scope renderable from an explicit name-to-value mapping.
     */
    public static Scope of(Map<String, Object> scope, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Scope(scope, cfg);
    }

    /**
     * Create a scope renderable from the instance fields of an object.
     * Non-static fields (including private ones, when accessible) are
     * collected into a name-to-value mapping.
     */
    public static Scope ofObject(Object target, Consumer<Config> configurer) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    fields.put(field.getName(), field.get(target));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Unreadable field (e.g. module access restrictions)
                    fields.put(field.getName(), "<unavailable>");
                }
            }
            clazz = clazz.getSuperclass();
        }
        return of(fields, configurer);
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    private Panel buildPanel(Console console) {
        Table itemsTable = Table.grid(0, 1, false);
        // Key column, right-justified — value column is implicit
        itemsTable.addColumn(null, null, "right");

        List<Map.Entry<String, Object>> items = new ArrayList<>(scope.entrySet());
        if (sortKeys) {
            // Special variables (starting with "__") first, then alphabetically,
            // matching Python's sort key: (not key.startswith("__"), key.lower())
            items.sort(Comparator
                    .comparing((Map.Entry<String, Object> e) -> !e.getKey().startsWith("__"))
                    .thenComparing(e -> e.getKey().toLowerCase(Locale.ROOT)));
        }

        Style specialStyle = console.getStyle("scope.key.special");
        Style keyStyle = console.getStyle("scope.key");
        Style equalsStyle = console.getStyle("scope.equals");

        for (Map.Entry<String, Object> item : items) {
            String key = item.getKey();
            Text keyText = Text.assemble(
                    key, key.startsWith("__") ? specialStyle : keyStyle,
                    " =", equalsStyle);
            itemsTable.addRow(keyText, new Pretty(item.getValue(), true, indentGuides));
        }
        return Panel.fit(itemsTable, title, cfg -> cfg.borderStyle("scope.border"));
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        return buildPanel(console).richConsole(console, options);
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        return buildPanel(console).richMeasure(console, options);
    }
}
