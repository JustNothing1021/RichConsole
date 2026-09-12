package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders the time elapsed since the task started.
 * Ported from rich/progress.py TimeElapsedColumn.
 */
public class TimeElapsedColumn extends ProgressColumn {

    @Override
    public Object render(Progress.Task task) {
        Double elapsed = task.getElapsed();
        if (elapsed == null) {
            return new Text("-:--:--", "progress.elapsed");
        }
        return new Text(ProgressFormats.formatTime(elapsed), "progress.elapsed");
    }
}
