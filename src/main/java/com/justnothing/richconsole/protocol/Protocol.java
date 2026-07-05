package com.justnothing.richconsole.protocol;

import java.util.HashSet;
import java.util.Set;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;

/**
 * Protocol utility functions.
 * Ported from rich/protocol.py.
 */
public final class Protocol {

    private Protocol() {}

    /**
     * Check if an object may be rendered by Rich.
     *
     * @param obj the object to check
     * @return true if the object is renderable
     */
    public static boolean isRenderable(Object obj) {
        return obj instanceof String
            || obj instanceof RichCast
            || obj instanceof RichRenderable;
    }

    // Import for RichRenderable check via reflection
    private static final String RICH_RENDERABLE_METHOD = "richConsole";
    private static final String RICH_CAST_METHOD = "rich";

    /**
     * Check if an object may be rendered by Rich (including duck-typing checks).
     * This version also checks for method presence via reflection for objects
     * that don't implement our interfaces directly.
     *
     * @param obj the object to check
     * @return true if the object is renderable
     */
    public static boolean isRenderableReflective(Object obj) {
        if (obj == null) return false;
        if (obj instanceof String) return true;
        if (obj instanceof RichCast) return true;
        if (obj instanceof RichRenderable) return true;

        // Duck-typing: check for rich() or richConsole() methods
        try {
            obj.getClass().getMethod(RICH_CAST_METHOD);
            return true;
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            obj.getClass().getMethod(RICH_RENDERABLE_METHOD, Console.class, ConsoleOptions.class);
            return true;
        } catch (NoSuchMethodException e) {
            // ignore
        }
        return false;
    }

    /**
     * Cast an object to a renderable by calling rich() if present.
     * Recursively applies rich() until a non-RichCast object is obtained.
     *
     * @param renderable a potentially renderable object
     * @return the result of recursively calling rich()
     */
    public static Object richCast(Object renderable) {
        Set<Class<?>> visitedTypes = new HashSet<>();

        while (renderable instanceof RichCast) {
            Object result = ((RichCast) renderable).rich();
            if (result == null) {
                // rich() returned null — stop recursion and fall back to toString
                return renderable.toString();
            }
            Class<?> resultType = result.getClass();
            if (visitedTypes.contains(resultType)) {
                break;
            }
            visitedTypes.add(resultType);
            renderable = result;
        }

        return renderable;
    }
}
