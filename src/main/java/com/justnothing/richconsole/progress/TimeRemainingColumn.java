package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders the estimated time remaining.
 * Ported from rich/progress.py TimeRemainingColumn.
 */
public class TimeRemainingColumn extends ProgressColumn {

    private final boolean compact;
    private final boolean elapsedWhenFinished;

    public TimeRemainingColumn() {
        this(false, false);
    }

    /**
     * @param compact             render MM:SS when the remaining time is under an hour
     * @param elapsedWhenFinished render time elapsed when the task is finished
     */
    public TimeRemainingColumn(boolean compact, boolean elapsedWhenFinished) {
        this.compact = compact;
        this.elapsedWhenFinished = elapsedWhenFinished;
        // Only refresh twice a second to prevent jitter (mirrors rich).
        setMaxRefresh(0.5);
    }

    @Override
    public Object render(Progress.Task task) {
        Double taskTime;
        String style;
        if (elapsedWhenFinished && task.isFinished()) {
            taskTime = task.getFinishedTime();
            style = "progress.elapsed";
        } else {
            taskTime = task.getTimeRemaining();
            style = "progress.remaining";
        }

        if (taskTime == null) {
            return new Text(compact ? "--:--" : "-:--:--", style);
        }

        int totalSeconds = (int) Math.max(0.0, taskTime);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        int hours = minutes / 60;
        minutes = minutes % 60;

        String formatted;
        if (compact && hours == 0) {
            formatted = String.format("%02d:%02d", minutes, seconds);
        } else {
            formatted = String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return new Text(formatted, style);
    }
}
