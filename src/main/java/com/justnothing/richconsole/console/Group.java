package com.justnothing.richconsole.console;

import java.util.ArrayList;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;

/**
 * A renderable that groups multiple renderables together.
 * Ported from rich/console.py Group.
 */
public class Group implements RichRenderable {

    private final List<?> renderablesArg;
    private final boolean fit;
    private List<?> renderables;

    /**
     * Create a new Group.
     *
     * @param renderables the renderables to group
     * @param fit         whether the group should fit to available width
     */
    public Group(List<?> renderables, boolean fit) {
        this.renderablesArg = renderables;
        this.fit = fit;
        this.renderables = null;
    }

    public Group(List<?> renderables) {
        this(renderables, true);
    }

    /**
     * Get the renderables in this group. Lazily initialized.
     *
     * @return the list of renderables
     */
    public List<?> getRenderables() {
        if (renderables == null) {
            renderables = new ArrayList<>(renderablesArg);
        }
        return renderables;
    }

    public boolean isFit() {
        return fit;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        return getRenderables();
    }
}
