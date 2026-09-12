package com.justnothing.richconsole.progress;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for a widget used in the progress display.
 * Ported from rich/progress.py ProgressColumn.
 *
 * <p>A column renders one cell of the progress table for a given
 * {@link Progress.Task}. Subclasses implement {@link #render(Progress.Task)}
 * and may optionally constrain the table column width via {@link #getWidth()}.</p>
 */
public abstract class ProgressColumn {

    /** A cached renderable, together with the time it was produced. */
    private record Cached(double timestamp, Object renderable) {
    }

    private Double maxRefresh;
    private final Map<Integer, Cached> cache = new HashMap<>();

    /**
     * Called by Progress to obtain the renderable for a task.
     * Applies the optional max-refresh cache (mirroring rich's jitter control).
     *
     * @param task the task to render
     * @return a renderable for the task
     */
    public Object call(Progress.Task task) {
        double now = task.getTime();
        if (maxRefresh != null && task.getCompleted() == 0.0) {
            Cached cached = cache.get(task.getId());
            if (cached != null && cached.timestamp() + maxRefresh > now) {
                return cached.renderable();
            }
        }
        Object renderable = render(task);
        cache.put(task.getId(), new Cached(now, renderable));
        return renderable;
    }

    /**
     * Render the column content for the given task.
     *
     * @param task the task to render
     * @return any renderable (Text, ProgressBar, String, ...)
     */
    public abstract Object render(Progress.Task task);

    /**
     * A fixed width for the table column, or null to size from content.
     *
     * @return fixed column width, or null
     */
    public Integer getWidth() {
        return null;
    }

    /**
     * Set the maximum refresh rate (seconds between re-renders) used to
     * prevent flicker while the task has no completed work.
     *
     * @param seconds minimum interval between re-renders
     */
    protected void setMaxRefresh(double seconds) {
        this.maxRefresh = seconds;
    }
}
