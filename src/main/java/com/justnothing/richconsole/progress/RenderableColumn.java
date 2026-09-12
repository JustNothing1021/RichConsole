package com.justnothing.richconsole.progress;

/**
 * A column that always renders a fixed renderable.
 * Ported from rich/progress.py RenderableColumn.
 */
public class RenderableColumn extends ProgressColumn {

    private final Object renderable;

    public RenderableColumn() {
        this("");
    }

    public RenderableColumn(Object renderable) {
        this.renderable = renderable;
    }

    @Override
    public Object render(Progress.Task task) {
        return renderable;
    }
}
