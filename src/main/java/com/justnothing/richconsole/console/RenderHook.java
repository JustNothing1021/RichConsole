package com.justnothing.richconsole.console;

import java.util.List;

/**
 * Provides hooks into the render process.
 * Ported from rich/console.py RenderHook.
 *
 * <p>RenderHooks are called during Console.print() to allow
 * modifications to the list of renderables before they are
 * rendered. This is used by Live to insert cursor control
 * sequences and restore the live display after printing.</p>
 */
public interface RenderHook {

    /**
     * Process renderables before rendering.
     *
     * <p>This method can return a new list of renderables, or modify
     * and return the same list. Called by Console.print() for each
     * registered hook.</p>
     *
     * @param renderables the list of objects to render
     * @return a replacement list of renderables
     */
    List<Object> processRenderables(List<Object> renderables);
}