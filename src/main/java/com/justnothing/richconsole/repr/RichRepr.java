package com.justnothing.richconsole.repr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Rich representation utilities, ported from rich/repr.py.
 */
public final class RichRepr {

    private RichRepr() {}

    /**
     * Annotation for classes that support rich repr.
     * Apply to a class to auto-generate toString() from constructor parameters.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RichReprAnnotation {
        boolean angular() default false;
    }

    /**
     * The result type for rich_repr - a list of repr parts.
     * Each part is either a plain value, or a (key, value) pair, or a (key, value, default) triple.
     */
    public static class RichReprResult {
        private final List<Object> parts = new ArrayList<>();

        public void add(Object value) {
            parts.add(value);
        }

        public void add(String key, Object value) {
            parts.add(new Object[]{key, value});
        }

        public void add(String key, Object value, Object defaultValue) {
            parts.add(new Object[]{key, value, defaultValue});
        }

        public List<Object> getParts() {
            return parts;
        }
    }

    /**
     * Error during repr generation.
     */
    public static class ReprError extends RuntimeException {
        public ReprError(String message) {
            super(message);
        }
    }

    /**
     * Auto-generate a rich toString() from a class's constructor parameters.
     * This is called from classes annotated with @RichReprAnnotation.
     */
    public static String autoToString(Object obj) {
        Class<?> clazz = obj.getClass();
        boolean angular = clazz.isAnnotationPresent(RichReprAnnotation.class)
            && clazz.getAnnotation(RichReprAnnotation.class).angular();

        RichReprResult result = autoRepr(obj);
        List<String> reprParts = new ArrayList<>();

        for (Object part : result.getParts()) {
            if (part instanceof Object[] triple) {
                if (triple.length == 2) {
                    String key = (String) triple[0];
                    Object value = triple[1];
                    reprParts.add(key + "=" + reprValue(value));
                } else if (triple.length == 3) {
                    String key = (String) triple[0];
                    Object value = triple[1];
                    Object defaultValue = triple[2];
                    // Skip if value equals default
                    if (value != null && value.equals(defaultValue)) {
                        continue;
                    }
                    reprParts.add(key + "=" + reprValue(value));
                }
            } else {
                reprParts.add(reprValue(part));
            }
        }

        if (angular) {
            return "<" + clazz.getSimpleName() + " " + String.join(" ", reprParts) + ">";
        } else {
            return clazz.getSimpleName() + "(" + String.join(", ", reprParts) + ")";
        }
    }

    private static String reprValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    /**
     * Auto-generate rich repr result from a class's declared fields using reflection.
     * Unlike Python rich which uses __match_args__ / constructor parameters,
     * Java doesn't reliably preserve parameter names without -parameters flag,
     * so we use declared fields instead.
     */
    public static RichReprResult autoRepr(Object obj) {
        RichReprResult result = new RichReprResult();
        Class<?> clazz = obj.getClass();

        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                String name = field.getName();
                Object value = field.get(obj);

                // Skip synthetic fields (like this$0 for inner classes)
                if (name.startsWith("this$")) {
                    continue;
                }

                // Try to get default value from field type
                Object defaultValue = null;
                Class<?> type = field.getType();
                if (type.isPrimitive()) {
                    if (type == int.class) defaultValue = 0;
                    else if (type == long.class) defaultValue = 0L;
                    else if (type == double.class) defaultValue = 0.0;
                    else if (type == float.class) defaultValue = 0.0f;
                    else if (type == boolean.class) defaultValue = false;
                    else if (type == char.class) defaultValue = '\0';
                    else if (type == short.class) defaultValue = (short) 0;
                    else if (type == byte.class) defaultValue = (byte) 0;
                }

                if (defaultValue != null && !defaultValue.equals(value)) {
                    result.add(name, value);
                } else if (defaultValue == null && value != null) {
                    result.add(name, value);
                } else {
                    result.add(name, value, defaultValue);
                }
            }
        } catch (Exception e) {
            throw new ReprError("Failed to auto generate rich repr; " + e.getMessage());
        }

        return result;
    }

}
