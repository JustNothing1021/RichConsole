package com.justnothing.richconsole.inspect;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.pretty.Pretty;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;

/**
 * Render an object's fields and (optionally) methods, similar to Python rich's
 * {@code inspect()}.
 *
 * <p>Java has no {@code dir()} / {@code getattr()} builtins, so attributes are
 * collected via reflection: public and declared instance fields, plus public
 * methods when {@code methods} is enabled. Each attribute is read with a safe
 * try/catch so a broken getter renders an error instead of failing.</p>
 *
 * <p>Unlike Python rich's Python-flavored {@code def name(...)} signatures, this
 * port prints Java signatures: fields as {@code (modifiers TypeName) value} and
 * methods as {@code public static <T extends X> ReturnType name(params) throws ...},
 * using the same VSCode-style palette as syntax highlighting (modifiers blue,
 * types green, names yellow, keywords blue, punctuation magenta).</p>
 */
public class Inspect implements RichRenderable {

    /**
     * Fluent configuration.
     * <pre>{@code
     * Inspect.of(myObject, cfg -> cfg.methods(true).dunder(true).title("my object"))
     * }</pre>
     */
    public static class Config {
        public Object title;
        public boolean methods = false;
        public boolean private_ = false;
        public boolean dunder = false;
        public boolean sort = true;
        public boolean value = true;
        /** Per-instance style overrides, keyed by style name (e.g. "type", "punct"), value a Style or style string. */
        public Map<String, Object> styles;

        public Config title(Object title) { this.title = title; return this; }
        public Config methods(boolean methods) { this.methods = methods; return this; }
        public Config private_(boolean private_) { this.private_ = private_; return this; }
        public Config dunder(boolean dunder) { this.dunder = dunder; return this; }
        public Config sort(boolean sort) { this.sort = sort; return this; }
        public Config value(boolean value) { this.value = value; return this; }
        public Config styles(Map<String, Object> styles) { this.styles = styles; return this; }
    }

    private final Object obj;
    private final Object title;
    private final boolean methods;
    private final boolean showPrivate;
    private final boolean showDunder;
    private final boolean sort;
    private final boolean value;
    private final Map<String, Object> styles;

    private Inspect(Object obj, Config cfg) {
        this.obj = obj;
        this.title = cfg.title != null ? cfg.title : defaultTitle(obj);
        this.methods = cfg.methods;
        this.showPrivate = cfg.private_;
        this.showDunder = cfg.dunder;
        this.sort = cfg.sort;
        this.value = cfg.value;
        this.styles = cfg.styles;
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    public static Inspect of(Object obj) {
        return of(obj, cfg -> {});
    }

    public static Inspect of(Object obj, Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new Inspect(obj, cfg);
    }

    /** Default title: the object's class name, matching Python's str(type(obj)). */
    private static String defaultTitle(Object obj) {
        return obj != null ? obj.getClass().getSimpleName() : "null";
    }

    // =========================================================================
    // Java signature helpers
    // =========================================================================

    /**
     * Resolve a style: per-instance override (Style or style string) wins,
     * otherwise fall back to {@code fallbackKey} on the console theme.
     */
    private static Style resolveStyle(Console console, Map<String, Object> overrides, String key, String fallbackKey) {
        if (overrides != null) {
            Object override = overrides.get(key);
            if (override != null) {
                return override instanceof Style s ? s : Style.parse(String.valueOf(override));
            }
        }
        return console.getStyle(fallbackKey);
    }

    /** Resolve a signature style with the default {@code inspect.<key>} fallback. */
    private static Style resolveStyle(Console console, Map<String, Object> overrides, String key) {
        return resolveStyle(console, overrides, key, "inspect." + key);
    }

    /** Theme styles used to paint Java signatures. */
    private static final class Sig {
        final Style modifier;
        final Style type;
        final Style name;
        final Style keyword;
        final Style punct;
        final Style sep;

        Sig(Console console, Map<String, Object> styles) {
            modifier = resolveStyle(console, styles, "modifier");
            type = resolveStyle(console, styles, "type");
            name = resolveStyle(console, styles, "name");
            keyword = resolveStyle(console, styles, "keyword");
            punct = resolveStyle(console, styles, "punct");
            sep = resolveStyle(console, styles, "sep");
        }
    }

    /**
     * Append "public static final " style modifiers (blue) to {@code parts}.
     * Field-only modifiers (volatile/transient) and method-only modifiers
     * (synchronized/native/abstract/strictfp) are checked per member kind,
     * because the JVM reuses access-flag bits (e.g. ACC_TRANSIENT == ACC_VARARGS).
     */
    private static void appendModifiers(List<Object> parts, int mods, boolean field, Sig s) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(mods)) {
            sb.append("public ");
        } else if (Modifier.isPrivate(mods)) {
            sb.append("private ");
        } else if (Modifier.isProtected(mods)) {
            sb.append("protected ");
        } else {
            sb.append("[package-private] ");
        }
        if (Modifier.isStatic(mods)) sb.append("static ");
        if (Modifier.isFinal(mods)) sb.append("final ");
        if (field) {
            if (Modifier.isVolatile(mods)) sb.append("volatile ");
            if (Modifier.isTransient(mods)) sb.append("transient ");
        } else {
            if (Modifier.isAbstract(mods)) sb.append("abstract ");
            if (Modifier.isSynchronized(mods)) sb.append("synchronized ");
            if (Modifier.isNative(mods)) sb.append("native ");
            if (Modifier.isStrict(mods)) sb.append("strictfp ");
        }
        if (sb.length() > 0) {
            parts.add(sb.toString().stripTrailing());
            parts.add(s.modifier);
            parts.add(" ");
            parts.add(Style.nullStyle());
        }
    }

    /**
     * Append a styled type (handles arrays, generics, wildcards and type
     * variables), mirroring DescriptorColorizer's generic-aware printing.
     */
    private static void appendType(List<Object> parts, Type type, Sig s) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                appendType(parts, clazz.getComponentType(), s);
                parts.add("[]");
                parts.add(s.punct);
            } else {
                parts.add(clazz.getSimpleName());
                parts.add(s.type);
            }
        } else if (type instanceof ParameterizedType pt) {
            appendType(parts, pt.getRawType(), s);
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                parts.add("<");
                parts.add(s.punct);
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        parts.add(", ");
                        parts.add(s.sep);
                    }
                    appendType(parts, args[i], s);
                }
                parts.add(">");
                parts.add(s.punct);
            }
        } else if (type instanceof GenericArrayType gat) {
            appendType(parts, gat.getGenericComponentType(), s);
            parts.add("[]");
            parts.add(s.punct);
        } else if (type instanceof WildcardType wt) {
            Type[] upper = wt.getUpperBounds();
            Type[] lower = wt.getLowerBounds();
            if (lower.length > 0) {
                parts.add("? super ");
                parts.add(s.keyword);
                appendType(parts, lower[0], s);
            } else if (upper.length > 0 && upper[0] != Object.class) {
                parts.add("? extends ");
                parts.add(s.keyword);
                appendType(parts, upper[0], s);
            } else {
                parts.add("?");
                parts.add(s.punct);
            }
        } else if (type instanceof TypeVariable<?> tv) {
            parts.add(tv.getName());
            parts.add(s.type);
        } else {
            parts.add(type.getTypeName());
            parts.add(s.type);
        }
    }

    /**
     * Full Java method signature, e.g.
     * {@code public static <T extends Comparable<T>> T max(List<T> values) throws IllegalArgumentException}.
     */
    private static Text methodText(Method method, Sig s) {
        List<Object> parts = new ArrayList<>();
        appendModifiers(parts, method.getModifiers(), false, s);

        // Type parameters: <T extends Bound1 & Bound2>
        TypeVariable<?>[] typeParams = method.getTypeParameters();
        if (typeParams.length > 0) {
            parts.add("<");
            parts.add(s.punct);
            for (int i = 0; i < typeParams.length; i++) {
                if (i > 0) {
                    parts.add(", ");
                    parts.add(s.sep);
                }
                parts.add(typeParams[i].getName());
                parts.add(s.type);
                Type[] bounds = typeParams[i].getBounds();
                if (bounds.length > 0 && !(bounds.length == 1 && bounds[0] == Object.class)) {
                    parts.add(" extends ");
                    parts.add(s.keyword);
                    for (int j = 0; j < bounds.length; j++) {
                        if (j > 0) {
                            parts.add(" & ");
                            parts.add(s.sep);
                        }
                        appendType(parts, bounds[j], s);
                    }
                }
            }
            parts.add(">");
            parts.add(s.punct);
            parts.add(" ");
            parts.add(Style.nullStyle());
        }

        appendType(parts, method.getGenericReturnType(), s);
        parts.add(" ");
        parts.add(Style.nullStyle());
        parts.add(method.getName());
        parts.add(s.name);
        parts.add("(");
        parts.add(s.punct);

        // Parameters, with varargs rendered as "Type..."
        Type[] params = method.getGenericParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                parts.add(", ");
                parts.add(s.sep);
            }
            if (method.isVarArgs() && i == params.length - 1) {
                Type param = params[i];
                if (param instanceof GenericArrayType gat) {
                    appendType(parts, gat.getGenericComponentType(), s);
                } else if (param instanceof Class<?> clazz && clazz.isArray()) {
                    appendType(parts, clazz.getComponentType(), s);
                } else {
                    appendType(parts, param, s);
                }
                parts.add("...");
                parts.add(s.punct);
            } else {
                appendType(parts, params[i], s);
            }
        }
        parts.add(")");
        parts.add(s.punct);

        // Throws clause
        Type[] exceptions = method.getGenericExceptionTypes();
        if (exceptions.length > 0) {
            parts.add(" throws ");
            parts.add(s.keyword);
            for (int i = 0; i < exceptions.length; i++) {
                if (i > 0) {
                    parts.add(", ");
                    parts.add(s.sep);
                }
                appendType(parts, exceptions[i], s);
            }
        }
        return Text.assemble(parts.toArray());
    }

    /** Field prefix "(modifiers TypeName) ", e.g. "(private final int) ". */
    private static Text fieldPrefix(Field field, Sig s) {
        List<Object> parts = new ArrayList<>();
        parts.add("(");
        parts.add(s.punct);
        appendModifiers(parts, field.getModifiers(), true, s);
        appendType(parts, field.getGenericType(), s);
        parts.add(")");
        parts.add(s.punct);
        parts.add(" ");
        parts.add(Style.nullStyle());
        return Text.assemble(parts.toArray());
    }

    /** Render a styled prefix followed inline by a value renderable (Pretty or error Text). */
    private static final class FieldValue implements RichRenderable {
        private final Object prefix;
        private final Object value;

        FieldValue(Object prefix, Object value) {
            this.prefix = prefix;
            this.value = value;
        }

        @Override
        public Iterable<?> richConsole(Console console, ConsoleOptions options) {
            List<Segment> result = new ArrayList<>();
            appendSegments(result, console, options, prefix);
            appendSegments(result, console, options, value);
            return result;
        }

        @Override
        public Measurement richMeasure(Console console, ConsoleOptions options) {
            Measurement p = Measurement.get(console, options, prefix);
            Measurement v = Measurement.get(console, options, value);
            return new Measurement(p.minimum() + v.minimum(), p.maximum() + v.maximum());
        }
    }

    /** Append a renderable's rendered segments to {@code out} (inline). */
    private static void appendSegments(List<Segment> out, Console console, ConsoleOptions options, Object renderable) {
        if (renderable instanceof RichRenderable rr) {
            for (Object item : rr.richConsole(console, options)) {
                out.add((Segment) item);
            }
        } else {
            for (Segment segment : console.render(renderable, options)) {
                out.add(segment);
            }
        }
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    /** An attribute discovered by reflection. */
    private static final class Attr {
        final String name;
        final boolean callable;
        final Field field;   // non-null for fields, null for callables
        final Object value;  // field value, or the Method for callables
        final Throwable error;

        Attr(String name, boolean callable, Field field, Object value, Throwable error) {
            this.name = name;
            this.callable = callable;
            this.field = field;
            this.value = value;
            this.error = error;
        }
    }

    private Group buildContent(Console console) {
        List<Object> renderables = new ArrayList<>();

        // Value panel: a Pretty rendering of the object itself
        if (value && obj != null && !(obj instanceof Class) && !isCallableObject(obj)) {
            Style valueBorder = resolveStyle(console, styles, "value.border");
            renderables.add(Panel.of(new Pretty(obj, true, true),
                    cfg -> cfg.borderStyle(valueBorder)));
            renderables.add("");
        }

        // Attribute table
        Table itemsTable = Table.grid(0, 1, false);
        itemsTable.addColumn(null, null, "right");

        List<Attr> attrs = collectAttributes();
        if (sort) {
            // Non-callable first, then by name with underscores stripped
            attrs.sort(Comparator
                    .comparing((Attr a) -> a.callable)
                    .thenComparing(a -> a.name.replace("_", "").toLowerCase(Locale.ROOT)));
        }

        Sig sig = new Sig(console, styles);
        Style attrStyle = resolveStyle(console, styles, "attr");
        Style dunderStyle = resolveStyle(console, styles, "attr.dunder");
        Style equalsStyle = resolveStyle(console, styles, "equals");
        Style errorStyle = resolveStyle(console, styles, "error");

        for (Attr attr : attrs) {
            Text keyText = Text.assemble(
                    attr.name, attr.name.startsWith("__") ? dunderStyle : attrStyle,
                    " =", equalsStyle);

            if (attr.error != null) {
                keyText.setStyle(errorStyle);
                Text errorText = Text.assemble(
                        "Cannot access: " + attr.error.getClass().getSimpleName(), errorStyle);
                itemsTable.addRow(keyText, new FieldValue(fieldPrefix(attr.field, sig), errorText));
                continue;
            }

            if (attr.callable) {
                itemsTable.addRow(keyText, methodText((Method) attr.value, sig));
            } else {
                itemsTable.addRow(keyText,
                        new FieldValue(fieldPrefix(attr.field, sig), new Pretty(attr.value, true, false)));
            }
        }

        renderables.add(itemsTable);
        return new Group(renderables);
    }

    private List<Attr> collectAttributes() {
        Map<String, Attr> attrs = new LinkedHashMap<>();
        Class<?> clazz = obj != null ? obj.getClass() : Object.class;

        // Fields: public (incl. inherited) + declared (to reach private)
        List<Field> fields = new ArrayList<>();
        if (obj instanceof Class<?> cls) {
            // Inspecting a Class object itself: use its declared fields
            fields.addAll(Arrays.asList(cls.getDeclaredFields()));
        } else {
            fields.addAll(Arrays.asList(clazz.getFields()));
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }
        for (Field field : fields) {
            String name = field.getName();
            if (!show(name)) {
                continue;
            }
            if (!attrs.containsKey(name)) {
                attrs.put(name, readField(field));
            }
        }

        // Methods (only when enabled)
        if (methods) {
            List<Method> methodList = new ArrayList<>();
            methodList.addAll(Arrays.asList(clazz.getMethods()));
            methodList.addAll(Arrays.asList(clazz.getDeclaredMethods()));
            for (Method method : methodList) {
                String name = method.getName();
                if (!show(name) || "getClass".equals(name)) {
                    continue;
                }
                if (!attrs.containsKey(name)) {
                    attrs.put(name, new Attr(name, true, null, method, null));
                }
            }
        }
        return new ArrayList<>(attrs.values());
    }

    private Attr readField(Field field) {
        String name = field.getName();
        try {
            Object value;
            if (obj instanceof Class<?> cls) {
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    value = field.get(null);
                } else {
                    return new Attr(name, false, field, null,
                            new IllegalStateException("cannot read instance field of a Class"));
                }
            } else {
                field.setAccessible(true);
                value = field.get(obj);
            }
            return new Attr(name, false, field, value, null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new Attr(name, false, field, null, e);
        }
    }

    private boolean show(String name) {
        if (!showDunder && name.startsWith("__")) {
            return false;
        }
        if (!showPrivate && name.startsWith("_")) {
            return false;
        }
        return true;
    }

    private static boolean isCallableObject(Object obj) {
        return obj instanceof Runnable || obj instanceof java.util.concurrent.Callable
                || obj instanceof java.util.function.Function;
    }

    // =========================================================================
    // Renderable
    // =========================================================================

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        return Panel.fit(buildContent(console), title,
                cfg -> cfg.borderStyle(borderStyle(console)).padding(0, 1)).richConsole(console, options);
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        return Panel.fit(buildContent(console), title,
                cfg -> cfg.borderStyle(borderStyle(console)).padding(0, 1)).richMeasure(console, options);
    }

    private Style borderStyle(Console console) {
        return resolveStyle(console, styles, "border", "scope.border");
    }
}
